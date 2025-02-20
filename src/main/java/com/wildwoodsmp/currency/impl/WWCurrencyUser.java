package com.wildwoodsmp.currency.impl;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.api.CurrencyApi;
import com.wildwoodsmp.currency.api.CurrencyUser;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WWCurrencyUser implements CurrencyUser {

    private final UUID playerId;
    private final Map<Currency, Double> balances;

    public WWCurrencyUser(UUID playerId) {
        this.playerId = playerId;
        this.balances = new HashMap<>();
    }

    public WWCurrencyUser(Document document) {
        this.playerId = UUID.fromString(document.getString("_id"));
        this.balances = new HashMap<>();
        for (String key : document.keySet()) {
            if (key.equals("_id")) continue;
            Optional<Currency> currency = CurrencyApi.getService().getCurrency(key);
            currency.ifPresent(value -> balances.put(value, document.getDouble(key)));
        }
    }

    public Document toDocument() {
        Document document = new Document();
        document.put("_id", playerId.toString());
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
    public void pay(Currency currency, double amount, String reason, UUID linkerId, String linkerReason) {
        throw new UnsupportedOperationException("Not implemented"); // TODO
    }

    @Override
    public void withdraw(Currency currency, double amount, String reason, UUID linkerId, String linkerReason) {
        throw new UnsupportedOperationException("Not implemented"); // TODO
    }

    @Override
    public void set(Currency currency, double amount, String reason, UUID linkerId, String linkerReason) {
        throw new UnsupportedOperationException("Not implemented"); // TODO
    }

    public Map<Currency, Double> getBalanceMap() {
        return balances;
    }
}
