package com.wildwoodsmp.currency.api;

import java.util.Map;
import java.util.UUID;

public class CurrencyApi {
    private final static CurrencyApi instance = new CurrencyApi();

    private Map<String, Currency> currencies;
    private Map<UUID, CurrencyUser> users;

    public static CurrencyApi get() {
        return instance;
    }

    public Map<String, Currency> getCurrencies() {
        return currencies;
    }

    public Map<UUID, CurrencyUser> getUsers() {
        return users;
    }

}
