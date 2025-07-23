package gg.sunken.currency.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import gg.sunken.currency.api.Currency;
import gg.sunken.currency.api.CurrencyService;
import gg.sunken.currency.api.CurrencyUser;
import gg.sunken.currency.impl.mongo.MongoProvider;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MongoCurrencyService implements CurrencyService {

    private final Map<String, Currency> currencies = new HashMap<>();
    private final Map<UUID, CurrencyUser> localUsersCache = new HashMap<>();
    private final MongoCollection<Document> userCollection;

    public MongoCurrencyService(String mongoUri, String mongoDatabase) {
        MongoClient mongoClient = MongoProvider.getMongoClient(mongoUri);
        this.userCollection = mongoClient.getDatabase(mongoDatabase).getCollection("users");
    }

    @Override
    public Map<String, Currency> currencies() {
        return currencies;
    }

    @Override
    public Optional<Currency> getCurrency(String name) {
        return Optional.ofNullable(currencies.get(name));
    }

    @Override
    public void addCurrency(Currency currency) {
        currencies.put(currency.name(), currency);
    }

    @Override
    public void removeCurrency(String name) {
        currencies.remove(name);
    }

    @Override
    public Map<UUID, CurrencyUser> localUsersCache() {
        return localUsersCache;
    }

    @Override
    public Optional<CurrencyUser> getCachedUser(UUID uuid) {
        return Optional.ofNullable(localUsersCache.get(uuid));
    }

    @Override
    public void addCachedUser(CurrencyUser user) {
        localUsersCache.put(user.userId(), user);
    }

    @Override
    public void removeCachedUser(UUID uuid) {
        localUsersCache.remove(uuid);
    }

    @Override
    public CompletableFuture<CurrencyUser> getUserFromDatabase(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document document = userCollection.find(new Document("_id", uuid.toString())).first();
            if (document == null) {
                return null;
            }
            return new MongoCurrencyUser(document);
        });
    }
}
