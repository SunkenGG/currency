package gg.sunken.currency.impl.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

public class MongoDriver {
    private final String uri;
    private final String databaseName;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDriver(String uri, String databaseName) {
        this.uri = uri;
        this.databaseName = databaseName;
    }

    public void connect() {
        if (mongoClient != null) {
            throw new IllegalStateException("Already connected to MongoDB");
        }
        mongoClient = MongoProvider.getMongoClient(uri);
        database = mongoClient.getDatabase(databaseName);
    }

    public void close() {
        if (mongoClient == null) {
            throw new IllegalStateException("Not connected to MongoDB");
        }
        MongoProvider.releaseMongoClient(uri);
        mongoClient = null;
    }

    public @NotNull MongoClient getMongoClient() {
        if (mongoClient == null) {
            throw new IllegalStateException("Not connected to MongoDB");
        }
        return mongoClient;
    }

    public MongoDatabase getDatabase() {
        if (mongoClient == null) {
            throw new IllegalStateException("Not connected to MongoDB");
        }
        return database;
    }

    public MongoCollection<Document> getUserCollection() {
        return database.getCollection("users");
    }

    public MongoCollection<Document> getTransactionCollection() {
        return database.getCollection("transactions");
    }

    public MongoCollection<Document> getDeletedTransactionCollection() {
        return database.getCollection("deleted_transactions");
    }
}
