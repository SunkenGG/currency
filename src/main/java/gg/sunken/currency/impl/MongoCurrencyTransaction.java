package gg.sunken.currency.impl;

import gg.sunken.currency.api.Currency;
import gg.sunken.currency.api.CurrencyApi;
import gg.sunken.currency.api.CurrencyTransaction;
import gg.sunken.currency.api.CurrencyTransactionType;
import org.bson.Document;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class MongoCurrencyTransaction implements CurrencyTransaction {
    private final Currency currency;
    private final UUID id;
    private final double amount;
    private final CurrencyTransactionType type;
    private final UUID user;
    private final String reason;
    private final Instant timestamp;
    private final UUID linkerId;
    private final String linkerReason;
    private boolean deleted;

    public MongoCurrencyTransaction(Currency currency, UUID id, double amount, CurrencyTransactionType type, UUID user, String reason, Instant timestamp, Optional<UUID> linkerId, Optional<String> linkerReason) {
        this.currency = currency;
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.user = user;
        this.reason = reason;
        this.timestamp = timestamp;
        this.linkerId = linkerId.orElse(null);
        this.linkerReason = linkerReason.orElse(null);
        this.deleted = false;
    }

    public MongoCurrencyTransaction(Document document) {
        this.currency = CurrencyApi.getService().currencies().get(document.getString("currency"));
        this.id = UUID.fromString(document.getString("_id"));
        this.amount = document.getDouble("amount");
        this.type = CurrencyTransactionType.valueOf(document.getString("type"));
        this.user = UUID.fromString(document.getString("user"));
        this.reason = document.getString("reason");
        this.timestamp = Instant.ofEpochMilli(document.getLong("timestamp"));
        this.linkerId = document.containsKey("linkerId") ? UUID.fromString(document.getString("linkerId")) : null;
        this.linkerReason = document.containsKey("linkerReason") ? document.getString("linkerReason") : null;
        this.deleted = document.containsKey("deleted") && document.getBoolean("deleted");
    }

    public Document toDocument() {
        Document document = new Document();
        document.put("currency", currency.name());
        document.put("_id", id.toString());
        document.put("amount", amount);
        document.put("type", type.name());
        document.put("user", user.toString());
        document.put("reason", reason);
        document.put("timestamp", timestamp.toEpochMilli());
        if (linkerId != null) {
            document.put("linkerId", linkerId.toString());
        }
        if (linkerReason != null) {
            document.put("linkerReason", linkerReason);
        }
        document.put("deleted", deleted);
        return document;
    }

    @Override
    public Currency currency() {
        return currency;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public double amount() {
        return amount;
    }

    @Override
    public CurrencyTransactionType type() {
        return type;
    }

    @Override
    public UUID user() {
        return user;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public Optional<UUID> linkerId() {
        return Optional.ofNullable(linkerId);
    }

    @Override
    public Optional<String> linkerReason() {
        return Optional.ofNullable(linkerReason);
    }

    public void delete() {
        this.deleted = true;
    }

    public boolean deleted() {
        return deleted;
    }
}
