package gg.sunken.currency.impl;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.api.CurrencyApi;
import gg.sunken.currency.api.CurrencyUser;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MongoCurrencyUser implements CurrencyUser {

    private final UUID playerId;
    private final String name;
    private final Map<Currency, Double> balances;

    public MongoCurrencyUser(UUID playerId, String name) {
        this.playerId = playerId;
        this.name = name;
        this.balances = new HashMap<>();
    }

    public MongoCurrencyUser(Document document) {
        this.playerId = UUID.fromString(document.getString("_id"));
        this.name = document.getString("name");
        this.balances = new HashMap<>();
        for (String key : document.keySet()) {
            if (key.equals("_id")) continue;
            if (key.equals("name")) continue;
            Optional<Currency> currency = CurrencyApi.getService().getCurrency(key);
            currency.ifPresent(value -> balances.put(value, document.getDouble(key)));
        }
    }

    public Document toDocument() {
        Document document = new Document();
        document.put("_id", playerId.toString());
        document.put("name", name);
        for (Map.Entry<Currency, Double> entry : balances.entrySet()) {
            document.put(entry.getKey().name(), entry.getValue());
        }
        return document;
    }

    @Override
    public UUID userId() {
        return playerId;
    }

    @Override
    public double balance(Currency currency) {
        return balances.getOrDefault(currency, 0.0);
    }

    @Override
    public void deposit(Currency currency, double amount, String reason, UUID linkerId, String linkerReason) {
        this.balances.put(currency, this.balances.getOrDefault(currency, 0.0) + amount);
    }

    @Override
    public void withdraw(Currency currency, double amount, String reason, UUID linkerId, String linkerReason) {
        this.balances.put(currency, this.balances.getOrDefault(currency, 0.0) - amount);
    }

    @Override
    public void set(Currency currency, double amount, String reason, UUID linkerId, String linkerReason) {
        this.balances.put(currency, amount);
    }

    public Map<Currency, Double> getBalanceMap() {
        return balances;
    }
}
