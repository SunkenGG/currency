package com.wildwoodsmp.currency.impl;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.wildwoodsmp.currency.api.*;
import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.impl.mongo.MongoDriver;
import com.wildwoodsmp.currency.util.SortedList;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WWCurrency implements Currency {
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

    public WWCurrency(String name, String plural, String symbol, boolean allowsNegatives, boolean allowsPay, String format, double defaultBalance, String mongoUri, String mongoDatabaseName) {
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

        WWCurrencyUser user;
        if (userDocument == null) {
            user = new WWCurrencyUser(uuid, Bukkit.getOfflinePlayer(uuid).getName());
            for (Currency value : CurrencyApi.getService().currencies().values()) {
                user.set(value, value.defaultBalance(), "Default balance", null, null);
            }
            userCollection.insertOne(user.toDocument());

            return 0;
        }

        user = new WWCurrencyUser(userDocument);
        return user.balance(this);
    }

    @Override
    public CurrencyTransaction deposit(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");

        WWCurrencyTransaction transaction = new WWCurrencyTransaction(
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

        WWCurrencyTransaction transaction = new WWCurrencyTransaction(
                this,
                UUID.randomUUID(),
                amount,
                CurrencyTransactionType.OVERRIDE,
                user,
                reason,
                Instant.now(),
                Optional.ofNullable(linkerId),
                Optional.ofNullable(linkerReason)
        );

        transactionCollection.insertOne(transaction.toDocument());
        userCollection.updateOne(new Document("_id", user.toString()), new Document("$set", new Document(this.name, amount)), new UpdateOptions().upsert(true));

        return transaction;
    }

    @Override
    public CurrencyTransaction withdraw(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");
        if (balance(user) - amount < 0 && !allowsNegatives) {
            throw new IllegalArgumentException("Cannot withdraw more than the balance");
        }

        WWCurrencyTransaction transaction = new WWCurrencyTransaction(
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
        transaction.put("deleted", true);

        if (transaction.containsKey("linkerId")) {
            getLinkedTransactions(UUID.fromString(transaction.getString("linkerId"))).forEach(linkedTransaction -> {
                if (((WWCurrencyTransaction) linkedTransaction).deleted()) {
                    return;
                }

                transactionCollection.deleteOne(new Document("_id", linkedTransaction.id().toString()));

                Document document = ((WWCurrencyTransaction) linkedTransaction).toDocument();
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
        transaction.put("deleted", false);

        if (transaction.containsKey("linkerId")) {
            getLinkedTransactions(UUID.fromString(transaction.getString("linkerId"))).forEach(linkedTransaction -> {
                if (!((WWCurrencyTransaction) linkedTransaction).deleted()) {
                    return;
                }

                deletedTransactionCollection.deleteOne(new Document("_id", linkedTransaction.id().toString()));

                Document document = ((WWCurrencyTransaction) linkedTransaction).toDocument();
                document.put("deleted", false);

                transactionCollection.insertOne(document);
            });
        }

        transactionCollection.insertOne(transaction);
        deletedTransactionCollection.deleteOne(new Document("_id", transactionId.toString()));
    }

    @Override
    public List<CurrencyTransaction> getTransactions(UUID user, int limit, int skip) {
        List<CurrencyTransaction> transactions = new SortedList<>(Comparator.comparing(CurrencyTransaction::timestamp));
        transactionCollection.find(new Document("user", user.toString()))
                .sort(new Document("timestamp", -1))
                .skip(skip)
                .limit(limit)
                .forEach(doc -> transactions.add(new WWCurrencyTransaction(doc)));

        return transactions;
    }

    @Override
    public List<CurrencyTransaction> getDeletedTransactions(UUID user, int limit, int skip) {
        List<CurrencyTransaction> transactions = new SortedList<>(Comparator.comparing(CurrencyTransaction::timestamp));
        deletedTransactionCollection.find(new Document("user", user.toString()))
                .sort(new Document("timestamp", -1))
                .skip(skip)
                .limit(limit)
                .forEach(doc -> transactions.add(new WWCurrencyTransaction(doc)));

        return transactions;
    }

    @Override
    public CurrencyTransaction getTransaction(UUID transactionId) {
        Document document = transactionCollection.find(new Document("_id", transactionId.toString())).first();
        if (document == null) return null;

        return new WWCurrencyTransaction(document);
    }

    @Override
    public CurrencyTransaction getDeletedTransaction(UUID transactionId) {
        Document document = deletedTransactionCollection.find(new Document("_id", transactionId.toString())).first();
        if (document == null) return null;

        return new WWCurrencyTransaction(document);
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
    public List<UUID> recalculateBalance(UUID user, Currency currency, int depth) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (currency == null) throw new IllegalArgumentException("Currency cannot be null");
        if (depth < 0) throw new IllegalArgumentException("Depth cannot be less than 0");
        if (depth > MAX_DEPTH) return List.of();

        List<UUID> recalculatedUsers = new ArrayList<>();
        double startingBalance = balance(user);
        double balance = 0;
        FindIterable<Document> documents = transactionCollection.find(new Document("user", user.toString()).append("currency", currency.name()));
        for (Document document : documents) {
            WWCurrencyTransaction transaction = new WWCurrencyTransaction(document);
            switch (transaction.type()) {
                case PAYMENT -> balance += transaction.amount();
                case WITHDRAWAL -> balance -= transaction.amount();
                case OVERRIDE -> balance = transaction.amount();
            }

            if (transaction.linkerId().isPresent()) {
                getLinkedTransactions(transaction.linkerId().get()).forEach(linkedTransaction -> {
                    if (recalculatedUsers.contains(linkedTransaction.user())) {
                        return;
                    }

                    recalculatedUsers.addAll(recalculateBalance(linkedTransaction.user(), currency, depth+1));
                });
            }
        }

        if (balance != startingBalance) {
            set(user, balance, "Recalculated balance", null, null);
        }

        recalculatedUsers.add(user);
        return recalculatedUsers;
    }

    @Override
    public void recount(UUID user, Currency currency) {
        List<CurrencyTransaction> list = history(user, currency);
        double balance = 0;
        for (CurrencyTransaction transaction : list) {
            switch (transaction.type()) {
                case PAYMENT -> balance += transaction.amount();
                case WITHDRAWAL -> balance -= transaction.amount();
                case OVERRIDE -> balance = transaction.amount();
            }
        }

        if (balance < 0 && !allowsNegatives) {
            balance = 0;
        }

        double finalBalance = balance;
        transaction(() -> {
            set(user, finalBalance, "Recounted balance", null, null); //TODO: verify this works
        });
    }

    @Override
    public List<CurrencyTransaction> history(UUID user, Currency currency) {
        List<CurrencyTransaction> transactions = new SortedList<>(Comparator.comparing(CurrencyTransaction::timestamp));
        FindIterable<Document> documents = transactionCollection.find(
                new Document("user", user.toString())
                        .append("currency", currency.name())
        );
        for (Document document : documents) {
            transactions.add(new WWCurrencyTransaction(document));
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
            transactions.add(new WWCurrencyTransaction(document));
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
            users.add(new WWCurrencyUser(document));
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
                session.abortTransaction();
                return false;
            } finally {
                session.close();
            }
        });
    }


}
