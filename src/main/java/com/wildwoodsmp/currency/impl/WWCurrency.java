package com.wildwoodsmp.currency.impl;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.wildwoodsmp.currency.api.*;
import com.wildwoodsmp.currency.impl.mongo.MongoDriver;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WWCurrency implements Currency {
    private final String name;
    private final String plural;
    private final String symbol;
    private final boolean allowsNegatives;
    private final boolean allowsPay;
    private final MongoDriver mongoDriver;
    private final MongoCollection<Document> userCollection;
    private final MongoCollection<Document> transactionCollection;
    private final MongoCollection<Document> deletedTransactionCollection;

    public WWCurrency(String name, String plural, String symbol, boolean allowsNegatives, boolean allowsPay, String mongoUri, String mongoDatabaseName) {
        this.name = name;
        this.plural = plural;
        this.symbol = symbol;
        this.allowsNegatives = allowsNegatives;
        this.allowsPay = allowsPay;

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
    public double balance(UUID uuid) {
        if (CurrencyApi.get().getUsers().containsKey(uuid)) {
            return CurrencyApi.get().getUsers().get(uuid).balance(this);
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
            CurrencyUser currencyUser = CurrencyApi.get().getUsers().get(user);
            if (currencyUser != null) {
                ((WWCurrencyUser) currencyUser).getBalanceMap().put(this, currencyUser.balance(this) + amount);
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
            CurrencyUser currencyUser = CurrencyApi.get().getUsers().get(user);
            if (currencyUser != null) {
                ((WWCurrencyUser) currencyUser).getBalanceMap().put(this, amount);
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
            CurrencyUser currencyUser = CurrencyApi.get().getUsers().get(user);
            if (currencyUser != null) {
                ((WWCurrencyUser) currencyUser).getBalanceMap().put(this, currencyUser.balance(this) - amount);
            }

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
        if (depth > 5) return List.of();

        List<UUID> recalculatedUsers = new ArrayList<>();
        double startingBalance = balance(user);
        double balance = 0;
        FindIterable<Document> documents = transactionCollection.find(new Document("user", user.toString()).append("currency", currency.name()));
        for (Document document : documents) {
            WWCurrencyTransaction transaction = new WWCurrencyTransaction(document);
            if (transaction.type() == CurrencyTransactionType.PAYMENT) {
                balance += transaction.amount();
            } else if (transaction.type() == CurrencyTransactionType.WITHDRAWAL) {
                balance -= transaction.amount();
            } if (transaction.type() == CurrencyTransactionType.OVERRIDE) {
                balance = transaction.amount();
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
    public List<CurrencyTransaction> history(UUID user, Currency currency) {
        List<CurrencyTransaction> transactions = new ArrayList<>();
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
