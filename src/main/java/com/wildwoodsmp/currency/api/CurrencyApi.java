package com.wildwoodsmp.currency.api;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class CurrencyApi {
    @Getter @Setter private static CurrencyService service;
}
