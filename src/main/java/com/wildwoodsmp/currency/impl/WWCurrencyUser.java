package com.wildwoodsmp.currency.impl;

import com.wildwoodsmp.currency.api.Currency;
import com.wildwoodsmp.currency.api.CurrencyApi;
import com.wildwoodsmp.currency.api.CurrencyUser;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
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
            balances.put(CurrencyApi.get().getCurrencies().get(key), document.getDouble(key));
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

    public Map<Currency, Double> getBalanceMap() {
        return balances;
    }
}
