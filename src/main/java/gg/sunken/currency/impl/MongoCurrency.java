package gg.sunken.currency.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.wildwoodsmp.currency.api.*;
import gg.sunken.currency.api.*;
import gg.sunken.currency.api.Currency;
import gg.sunken.currency.bukkit.CurrencyPlugin;
import gg.sunken.currency.impl.mongo.MongoDriver;
import gg.sunken.currency.util.SortedList;
import lombok.extern.java.Log;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log
public class MongoCurrency implements Currency {
    private final static int MAX_DEPTH = 5;
    private final String name;
    private final String plural;
    private final String symbol;
    private final boolean allowsNegatives;
    private final boolean allowsPay;
    private final String format;
    private final double defaultBalance;
    private final MongoDriver mongoDriver;
    private final MongoCollection<Document> userCollection;
    private final MongoCollection<Document> transactionCollection;
    private final MongoCollection<Document> deletedTransactionCollection;
    private final Cache<UUID, Object> recountCooldown = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public MongoCurrency(String name, String plural, String symbol, boolean allowsNegatives, boolean allowsPay, String format, double defaultBalance, String mongoUri, String mongoDatabaseName) {
        this.name = name;
        this.plural = plural;
        this.symbol = symbol;
        this.allowsNegatives = allowsNegatives;
        this.allowsPay = allowsPay;
        this.format = format;
        this.defaultBalance = defaultBalance;

        this.mongoDriver = new MongoDriver(mongoUri, mongoDatabaseName);
        this.mongoDriver.connect();

        this.userCollection = mongoDriver.getUserCollection();

        this.transactionCollection = mongoDriver.getTransactionCollection();
        this.transactionCollection.createIndex(new Document("linkerId", 1));
        this.transactionCollection.createIndex(new Document("user", 1));
        this.transactionCollection.createIndex(new Document("currency", 1));

        this.deletedTransactionCollection = mongoDriver.getDeletedTransactionCollection();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String plural() {
        return plural;
    }

    @Override
    public String symbol() {
        return symbol;
    }

    @Override
    public boolean allowsNegatives() {
        return allowsNegatives;
    }

    @Override
    public boolean allowsPay() {
        return allowsPay;
    }

    @Override
    public String format() {
        return format;
    }

    @Override
    public double defaultBalance() {
        return defaultBalance;
    }

    @Override
    public double balance(UUID uuid) {
        Map<UUID, CurrencyUser> cache = CurrencyApi.getService().localUsersCache();
        if (cache.containsKey(uuid)) {
            return cache.get(uuid).balance(this);
        }

        Document userDocument = userCollection.find()
                .filter(new Document("_id", uuid.toString()))
                .first();

        MongoCurrencyUser user;
        if (userDocument == null) {
            user = new MongoCurrencyUser(uuid, Bukkit.getOfflinePlayer(uuid).getName());
            for (Currency value : CurrencyApi.getService().currencies().values()) {
                user.set(value, value.defaultBalance(), "Default balance", null, null);
            }
            userCollection.insertOne(user.toDocument());

            return 0;
        }

        user = new MongoCurrencyUser(userDocument);
        return user.balance(this);
    }

    @Override
    public CurrencyTransaction deposit(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");

        MongoCurrencyTransaction transaction = new MongoCurrencyTransaction(
                this,
                UUID.randomUUID(),
                amount,
                CurrencyTransactionType.PAYMENT,
                user,
                reason,
                Instant.now(),
                Optional.ofNullable(linkerId),
                Optional.ofNullable(linkerReason)
        );
        transactionCollection.insertOne(transaction.toDocument());
        userCollection.updateOne(new Document("_id", user.toString()), new Document("$inc", new Document(this.name, amount)), new UpdateOptions().upsert(true));

        return transaction;
    }

    @Override
    public CurrencyTransaction set(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount < 0) throw new IllegalArgumentException("Amount must be greater than or equal to 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");

        double balance = balance(user);

        double delta = balance > amount ? -(balance - amount) : (amount - balance);

        if (delta == 0) {
            throw new IllegalArgumentException("User balance is already " + amount);
        }

        if (delta > 0) {
            return deposit(user, delta, reason, linkerId, linkerReason);
        } else {
            return withdraw(user, Math.abs(delta), reason, linkerId, linkerReason);
        }
    }

    @Override
    public CurrencyTransaction withdraw(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");
        if (balance(user) - amount < 0 && !allowsNegatives) {
            throw new IllegalArgumentException("Cannot withdraw more than the balance");
        }

        MongoCurrencyTransaction transaction = new MongoCurrencyTransaction(
                this,
                UUID.randomUUID(),
                amount,
                CurrencyTransactionType.WITHDRAWAL,
                user,
                reason,
                Instant.now(),
                Optional.ofNullable(linkerId),
                Optional.ofNullable(linkerReason)
        );

        transactionCollection.insertOne(transaction.toDocument());
        userCollection.updateOne(new Document("_id", user.toString()), new Document("$inc", new Document(this.name, -amount)), new UpdateOptions().upsert(true));

        return transaction;
    }

    @Override
    public void invalidateTransaction(UUID transactionId) {
        if (transactionId == null) throw new IllegalArgumentException("Transaction ID cannot be null");

        Document transaction = transactionCollection.find(new Document("_id", transactionId.toString())).first();
        if (transaction == null) throw new IllegalArgumentException("Transaction not found");
        if (transaction.getBoolean("deleted")) throw new IllegalArgumentException("Transaction already deleted");
        transaction.put("deleted", true);

        if (transaction.containsKey("linkerId")) {
            getLinkedTransactions(UUID.fromString(transaction.getString("linkerId"))).forEach(linkedTransaction -> {
                if (((MongoCurrencyTransaction) linkedTransaction).deleted()) {
                    return;
                }

                transactionCollection.deleteOne(new Document("_id", linkedTransaction.id().toString()));

                Document document = ((MongoCurrencyTransaction) linkedTransaction).toDocument();
                document.put("deleted", true);

                deletedTransactionCollection.insertOne(document);
            });
        }

        deletedTransactionCollection.insertOne(transaction);
        transactionCollection.deleteOne(new Document("_id", transactionId.toString()));
    }

    @Override
    public void validateTransaction(UUID transactionId) {
        if (transactionId == null) throw new IllegalArgumentException("Transaction ID cannot be null");

        Document transaction = deletedTransactionCollection.find(new Document("_id", transactionId.toString())).first();
        if (transaction == null) throw new IllegalArgumentException("Transaction not found");
        if (!transaction.getBoolean("deleted")) throw new IllegalArgumentException("Transaction is not deleted");

        transaction.put("deleted", false);

        if (transaction.containsKey("linkerId")) {
            getLinkedTransactions(UUID.fromString(transaction.getString("linkerId"))).forEach(linkedTransaction -> {
                if (!((MongoCurrencyTransaction) linkedTransaction).deleted()) {
                    return;
                }

                deletedTransactionCollection.deleteOne(new Document("_id", linkedTransaction.id().toString()));

                Document document = ((MongoCurrencyTransaction) linkedTransaction).toDocument();
                document.put("deleted", false);

                transactionCollection.insertOne(document);
            });
        }

        transactionCollection.insertOne(transaction);
        deletedTransactionCollection.deleteOne(new Document("_id", transactionId.toString()));
    }

    @Override
    public List<CurrencyTransaction> getTransactions(UUID user, int limit, int skip) {
        List<CurrencyTransaction> transactions = new SortedList<>(Comparator.comparing(currencyTransaction -> {
            if (currencyTransaction instanceof MongoCurrencyTransaction) {
                return -currencyTransaction.timestamp().getEpochSecond();
            }
            return -Instant.now().getEpochSecond();
        }));
        transactionCollection.find(new Document("user", user.toString()))
                .sort(new Document("timestamp", -1))
                .skip(skip)
                .limit(limit)
                .forEach(doc -> transactions.add(new MongoCurrencyTransaction(doc)));

        return transactions;
    }

    @Override
    public List<CurrencyTransaction> getDeletedTransactions(UUID user, int limit, int skip) {
        List<CurrencyTransaction> transactions = new SortedList<>(Comparator.comparing(currencyTransaction -> {
            if (currencyTransaction instanceof MongoCurrencyTransaction) {
                return -currencyTransaction.timestamp().getEpochSecond();
            }
            return -Instant.now().getEpochSecond();
        }));
        deletedTransactionCollection.find(new Document("user", user.toString()))
                .sort(new Document("timestamp", -1))
                .skip(skip)
                .limit(limit)
                .forEach(doc -> transactions.add(new MongoCurrencyTransaction(doc)));

        return transactions;
    }

    @Override
    public CurrencyTransaction getTransaction(UUID transactionId) {
        Document document = transactionCollection.find(new Document("_id", transactionId.toString())).first();
        if (document == null) return null;

        return new MongoCurrencyTransaction(document);
    }

    @Override
    public CurrencyTransaction getDeletedTransaction(UUID transactionId) {
        Document document = deletedTransactionCollection.find(new Document("_id", transactionId.toString())).first();
        if (document == null) return null;

        return new MongoCurrencyTransaction(document);
    }

    @Override
    public long transactionsCount(UUID user) {
        return transactionCollection.countDocuments(new Document("user", user.toString()));
    }

    @Override
    public long deletedTransactionsCount(UUID user) {
        return deletedTransactionCollection.countDocuments(new Document("user", user.toString()));
    }

    @Override
    public List<UUID> recalculateBalance(UUID user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (recountCooldown.getIfPresent(user) != null) {
            return Collections.emptyList();
        }

        recountCooldown.put(user, new Object());

        List<UUID> recalculatedUsers = new ArrayList<>();
        recalculatedUsers.add(user);
        double startingBalance = balance(user);
        double balance = 0;
        FindIterable<Document> documents = transactionCollection.find(new Document("user", user.toString()).append("currency", this.name()));
        for (Document document : documents) {
            MongoCurrencyTransaction transaction = new MongoCurrencyTransaction(document);
            switch (transaction.type()) {
                case PAYMENT -> balance += transaction.amount();
                case WITHDRAWAL -> {
                    if (balance - transaction.amount() < 0 && !allowsNegatives) {
                        invalidateTransaction(transaction.id());
                        continue;
                    }
                    balance -= transaction.amount();
                }
                case OVERRIDE -> balance = transaction.amount();
            }

            if (transaction.linkerId().isPresent()) {
                getLinkedTransactions(transaction.linkerId().get()).forEach(linkedTransaction -> {
                    if (recalculatedUsers.contains(linkedTransaction.user())) {
                        return;
                    }

                    if (recountCooldown.getIfPresent(linkedTransaction.user()) != null) {
                        return;
                    }

                    recalculatedUsers.add(linkedTransaction.user());
                    Bukkit.getScheduler().runTaskLaterAsynchronously(CurrencyPlugin.getPlugin(CurrencyPlugin.class), () -> {
                        recalculateBalance(linkedTransaction.user());
                    }, 20L);
                });
            }
        }

        if (balance != startingBalance) {
            userCollection.updateOne(
                    new Document("_id", user.toString()),
                    new Document("$set", new Document(this.name, balance)),
                    new UpdateOptions().upsert(true)
            );
            double finalBalance = balance;
            forCacheUser(user, currencyUser -> {
                currencyUser.set(this,  finalBalance, "Recalculated balance", null, null);
            });
        }

        return recalculatedUsers;
    }

    @Override
    public List<CurrencyTransaction> history(UUID user) {
        List<CurrencyTransaction> transactions = new SortedList<>(Comparator.comparing(CurrencyTransaction::timestamp));
        FindIterable<Document> documents = transactionCollection.find(
                new Document("user", user.toString())
                        .append("currency", this.name())
        );
        for (Document document : documents) {
            transactions.add(new MongoCurrencyTransaction(document));
        }

        return transactions;
    }

    @Override
    public List<CurrencyTransaction> getLinkedTransactions(UUID linkerId) {
        List<CurrencyTransaction> transactions = new ArrayList<>();
        FindIterable<Document> documents = transactionCollection.find(
                new Document("linkerId", linkerId.toString())
        );
        for (Document document : documents) {
            transactions.add(new MongoCurrencyTransaction(document));
        }

        return transactions;
    }

    @Override
    public List<CurrencyUser> getTopBalances(int limit, int skip) {
        List<CurrencyUser> users = new ArrayList<>();
        FindIterable<Document> documents = userCollection.find()
                .sort(new Document(name, -1))
                .skip(skip)
                .limit(limit);
        for (Document document : documents) {
            users.add(new MongoCurrencyUser(document));
        }

        return users;
    }

    @Override
    public long currencyUserCount() {
        return userCollection.countDocuments();
    }

    @Override
    public CompletableFuture<Boolean> transaction(Runnable transactionRunnable) {
        return CompletableFuture.supplyAsync(() -> {
            ClientSession session = mongoDriver.getMongoClient().startSession();
            session.startTransaction();
            try {
                transactionRunnable.run();
                session.commitTransaction();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                session.abortTransaction();
                return false;
            } finally {
                session.close();
            }
        });
    }


}
