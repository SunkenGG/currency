package gg.sunken.currency.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CurrencyTransaction {
    /**
     * Get the currency of the transaction.
     * @return The currency of the transaction.
     */
    Currency currency();

    /**
     * Get the ID of the transaction.
     * @return The ID of the transaction.
     */
    UUID id();

    /**
     * Get the amount of currency in the transaction.
     * @return The amount of currency in the transaction.
     */
    double amount();

    /**
     * Get the type of transaction.
     * @return The type of transaction.
     */
    CurrencyTransactionType type();

    /**
     * Get the user involved in the transaction.
     * @return The UUID of the user involved in the transaction.
     */
    UUID user();

    /**
     * Get the reason for the transaction.
     * @return The reason for the transaction.
     */
    String reason();

    /**
     * Get the timestamp of the transaction.
     * @return The timestamp of the transaction.
     */
    Instant timestamp();

    /**
     * An id to link multiple transactions together.
     * @return The id of the transaction.
     */
    Optional<UUID> linkerId();

    /**
     * A reason to link multiple transactions together.
     * @return The reason of the transaction.
     */
    Optional<String> linkerReason();
}
