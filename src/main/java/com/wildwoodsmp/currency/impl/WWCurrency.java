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
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

public class WWCurrency implements Currency {
    private final static int MAX_DEPTH = 5;
    private final String name;
    private final String plural;
    private final String symbol;
    private final boolean allowsNegatives;
    private final boolean allowsPay;
    private final int decimalPlaces;
    private final String format;
    private final double defaultBalance;
    private final MongoDriver mongoDriver;
    private final MongoCollection<Document> userCollection;
    private final MongoCollection<Document> transactionCollection;
    private final MongoCollection<Document> deletedTransactionCollection;

    public WWCurrency(String name, String plural, String symbol, boolean allowsNegatives, boolean allowsPay, int decimalPlaces, String format, double defaultBalance, String mongoUri, String mongoDatabaseName) {
        this.name = name;
        this.plural = plural;
        this.symbol = symbol;
        this.allowsNegatives = allowsNegatives;
        this.allowsPay = allowsPay;
        this.decimalPlaces = decimalPlaces;
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
    public int decimalPlaces() {
        return decimalPlaces;
    }

    @Override
    public String format() {
        return format;
    }

    @Override
    public String format(double amount) {
        return String.format(format, amount);
    }

    @Override
    public double defaultBalance() {
        return defaultBalance;
    }

    @Override
    public double balance(UUID uuid) {
        if (CurrencyApi.getService().localUsersCache().containsKey(uuid)) {
            return CurrencyApi.getService().localUsersCache().get(uuid).balance(this);
        }

        Document userDocument = userCollection.find()
                .filter(new Document("_id", uuid.toString()))
                .first();

        WWCurrencyUser user;
        if (userDocument == null) {
            user = new WWCurrencyUser(uuid);
            userCollection.insertOne(user.toDocument());

            return 0;
        }

        user = new WWCurrencyUser(userDocument);
        return user.balance(this);
    }

    @Override
    public CurrencyTransaction pay(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");

        ClientSession session = mongoDriver.getMongoClient().startSession();
        session.startTransaction();
        WWCurrencyTransaction transaction;
        try {
            transaction = new WWCurrencyTransaction(
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

            transactionCollection.insertOne(session, transaction.toDocument());
            userCollection.updateOne(session, new Document("_id", user.toString()), new Document("$inc", new Document(this.name, amount)), new UpdateOptions().upsert(true));
            Optional<CurrencyUser> localUser = CurrencyApi.getService().getLocalUser(user);
            localUser.ifPresent(currencyUser -> ((WWCurrencyUser) currencyUser).getBalanceMap().put(this, currencyUser.balance(this) + amount));
            //TODO: Pubsub notify message

            session.commitTransaction();
        } catch (Exception e) {
            session.abortTransaction();
            throw e;
        } finally {
            session.close();
        }

        return transaction;
    }

    @Override
    public CurrencyTransaction set(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount < 0) throw new IllegalArgumentException("Amount must be greater than or equal to 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");

        ClientSession session = mongoDriver.getMongoClient().startSession();
        session.startTransaction();
        WWCurrencyTransaction transaction;
        try {
            transaction = new WWCurrencyTransaction(
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

            transactionCollection.insertOne(session, transaction.toDocument());
            userCollection.updateOne(session, new Document("_id", user.toString()), new Document("$set", new Document(this.name, amount)), new UpdateOptions().upsert(true));
            Optional<CurrencyUser> currencyUser = CurrencyApi.getService().getLocalUser(user);
            if (currencyUser.isPresent()) {
                ((WWCurrencyUser) currencyUser.get()).getBalanceMap().put(this, amount);
            }
            //TODO: Pubsub notify message

            session.commitTransaction();
        } catch (Exception e) {
            session.abortTransaction();
            throw e;
        } finally {
            session.close();
        }

        return transaction;
    }

    @Override
    public CurrencyTransaction withdraw(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");
        if (reason == null) throw new IllegalArgumentException("Reason cannot be null");

        ClientSession session = mongoDriver.getMongoClient().startSession();
        session.startTransaction();
        WWCurrencyTransaction transaction;
        try {
            transaction = new WWCurrencyTransaction(
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

            transactionCollection.insertOne(session, transaction.toDocument());
            userCollection.updateOne(session, new Document("_id", user.toString()), new Document("$inc", new Document(this.name, -amount)), new UpdateOptions().upsert(true));
            Optional<CurrencyUser> currencyUser = CurrencyApi.getService().getLocalUser(user);
            currencyUser.ifPresent(value -> ((WWCurrencyUser) value).getBalanceMap().put(this, value.balance(this) - amount));

        } catch (Exception e) {
            session.abortTransaction();
            throw e;
        } finally {
            session.close();
        }

        return transaction;
    }

    @Override
    public void invalidateTransaction(UUID transactionId) {
        if (transactionId == null) throw new IllegalArgumentException("Transaction ID cannot be null");

        ClientSession session = mongoDriver.getMongoClient().startSession();
        session.startTransaction();

        try {
            Document transaction = transactionCollection.find(session, new Document("_id", transactionId.toString())).first();
            if (transaction == null) throw new IllegalArgumentException("Transaction not found");

            deletedTransactionCollection.insertOne(session, transaction);
            transactionCollection.deleteOne(session, new Document("_id", transactionId.toString()));

            session.commitTransaction();
        } catch (Exception e) {
            session.abortTransaction();
            throw e;
        } finally {
            session.close();
        }
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

        set(user, balance, "Recounted balance", null, null); //TODO: verify this works
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
}
