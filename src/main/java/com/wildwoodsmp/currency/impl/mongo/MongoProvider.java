package com.wildwoodsmp.currency.impl.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.HashMap;
import java.util.Map;

public class MongoProvider {
    private final static Map<String, MongoClient> mongoClients = new HashMap<>();
    private final static Map<String, Integer> mongoClientReferences = new HashMap<>();

    public static MongoClient getMongoClient(String uri) {
        if (mongoClients.containsKey(uri)) {
            mongoClientReferences.put(uri, mongoClientReferences.get(uri) + 1);
            return mongoClients.get(uri);
        } else {
            MongoClient mongoClient = MongoClients.create(uri);
            mongoClients.put(uri, mongoClient);
            mongoClientReferences.put(uri, 1);
            return mongoClient;
        }
    }

    public static void releaseMongoClient(String uri) {
        if (mongoClientReferences.containsKey(uri)) {
            int references = mongoClientReferences.get(uri);
            if (references == 1) {
                mongoClients.get(uri).close();
                mongoClients.remove(uri);
                mongoClientReferences.remove(uri);
            } else {
                mongoClientReferences.put(uri, references - 1);
            }
        }
    }

}
