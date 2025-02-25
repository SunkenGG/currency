package com.wildwoodsmp.currency.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface Currency {

    /**
     * Get the name of the currency.
     * @return The name of the currency.
     */
    String name();

    /**
     * Get the plural form of the currency.
     * @return The plural form of the currency.
     */
    String plural();

    /**
     * Get the symbol of the currency.
     * @return The symbol of the currency.
     */
    String symbol();

    /**
     * Get whether the currency allows negative balances.
     * @return True if the currency allows negative balances, false otherwise.
     */
    boolean allowsNegatives();

    /**
     * Get whether the currency allows payments.
     * @return True if the currency allows payments, false otherwise.
     */
    boolean allowsPay();

    /**
     * Get the currency format.
     * @return The currency format.
     */
    String format();

    /**
     * Get the currency format for a certain amount.
     * @return The currency format for the amount.
     */
    String format(double amount);

    /**
     * Get the default balance of a player.
     * @return The default balance of a player.
     */
    double defaultBalance();

    /**
     * Get the balance of a player.
     * @param uuid The UUID of the player.
     * @return The balance of the player.
     */
    double balance(UUID uuid);

    /**
     * Pay a certain amount of currency from one player to another.
     * @param user The UUID of the player.
     * @param amount The amount of currency to pay.
     * @param reason The reason for the payment.
     * @param linkerId An id to link multiple transactions together.
     * @param linkerReason A reason to link multiple transactions together.
     * @return The transaction object representing the payment.
     */
    CurrencyTransaction deposit(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason);

    /**
     * Pay a certain amount of currency from one player to another.
     * @param user The UUID of the player.
     * @param amount The amount of currency to pay.
     * @param reason The reason for the payment.
     * @return The transaction object representing the payment.
     */
    default CurrencyTransaction deposit(UUID user, double amount, String reason) {
        return deposit(user, amount, reason, null, null);
    }

    /**
     * Set the balance of a player.
     * @param user The UUID of the player.
     * @param amount The amount to set the balance to.
     * @param reason The reason for the balance change.
     * @param linkerId An id to link multiple transactions together.
     * @param linkerReason A reason to link multiple transactions together.
     * @return The transaction object representing the balance change.
     */
    CurrencyTransaction set(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason);

    /**
     * Set the balance of a player.
     * @param user The UUID of the player.
     * @param amount The amount to set the balance to.
     * @param reason The reason for the balance change.
     * @return The transaction object representing the balance change.
     */
    default CurrencyTransaction set(UUID user, double amount, String reason) {
        return set(user, amount, reason, null, null);
    }

    /**
     * Withdraw a certain amount of currency from a player.
     * @param user The UUID of the player.
     * @param amount The amount to withdraw.
     * @param reason The reason for the withdrawal.
     * @param linkerId An id to link multiple transactions together.
     * @param linkerReason A reason to link multiple transactions together.
     * @return The transaction object representing the withdrawal.
     */
    CurrencyTransaction withdraw(UUID user, double amount, String reason, @Nullable UUID linkerId, @Nullable String linkerReason);

    /**
     * Withdraw a certain amount of currency from a player.
     * @param user The UUID of the player.
     * @param amount The amount to withdraw.
     * @param reason The reason for the withdrawal.
     * @return The transaction object representing the withdrawal.
     */
    default CurrencyTransaction withdraw(UUID user, double amount, String reason) {
        return withdraw(user, amount, reason, null, null);
    }

    /**
     * If the currency has a balance for a player.
     * @param uniqueId The UUID of the player.
     * @param amount The amount to check for.
     * @return True if the player has the amount, false otherwise.
     */
    default boolean has(@NotNull UUID uniqueId, double amount) {
        return balance(uniqueId) >= amount;
    }

    /**
     * Invalidate a transaction.
     * @param transactionId The ID of the transaction to invalidate.
     */
    void invalidateTransaction(UUID transactionId);

    /**
     * This will recount all transactions to recalculate the balannce of this user, this can
     * also cause a cascade effect of other players being recalculated too.
     * @param user The UUID of the player.
     * @param currency The currency to recalculate.
     * @return A list of UUIDs of players that were recalculated.
     */
    default List<UUID> recalculateBalance(UUID user, Currency currency) {
        return recalculateBalance(user, currency, 0);
    }

    List<UUID> recalculateBalance(UUID user, Currency currency, int depth);

    /**
     * Get the transaction history of a player.
     * @param user The UUID of the player.
     * @param currency The currency to get the history for.
     * @return A list of transactions for the player.
     */
    List<CurrencyTransaction> history(UUID user, Currency currency);

    /**
     * Recounts the balance of a user based off the transaction history.
     * @param user The UUID of the player.
     * @param currency The currency to recount.
     */
    void recount(UUID user, Currency currency);

    /**
     * Get the transaction history of a linker id
     * @param linkerId The UUID of the linker id.
     * @return A list of transactions for the linker id.
     */
    List<CurrencyTransaction> getLinkedTransactions(UUID linkerId);

    /**
     * Get the top balances of the currency.
     * @param limit The number of balances to get.
     * @param skip The number of balances to skip.
     * @return A list of currency users with the top balances.
     */
    List<CurrencyUser> getTopBalances(int limit, int skip);
}