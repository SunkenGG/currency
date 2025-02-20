package com.wildwoodsmp.currency.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CurrencyService {

    Map<String, Currency> currencies();

    Optional<Currency> getCurrency(String name);

    void addCurrency(Currency currency);

    void removeCurrency(String name);

    default void removeCurrency(Currency currency) {
        removeCurrency(currency.name());
    }

    Map<UUID, CurrencyUser> localUsersCache();

    Optional<CurrencyUser> getLocalUser(UUID uuid);

    void addLocalUser(CurrencyUser user);

    void removeLocalUser(UUID uuid);

    CompletableFuture<CurrencyUser> getUserFromDatabase(UUID uuid);
}
