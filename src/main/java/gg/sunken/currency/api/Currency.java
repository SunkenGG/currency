package gg.sunken.currency.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
    default String format(double amount) {
        return String.format(format(), amount);
    }

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
     * Validate a transaction.
     * @param transactionId The ID of the transaction to validate.
     */
    void validateTransaction(UUID transactionId);

    /**
     * Get the transactions of a player.
     * @param user The UUID of the player.
     * @param limit The number of transactions to get.
     * @param skip The number of transactions to skip.
     * @return A list of transactions for the player.
     */
    List<CurrencyTransaction> getTransactions(UUID user, int limit, int skip);

    /**
     * Get the transactions of a player.
     * @param user The UUID of the player.
     * @return A list of transactions for the player.
     */
    default List<CurrencyTransaction> getTransactions(UUID user) {
        return getTransactions(user, 0, 0);
    }

    /**
     * Get the deleted transactions of a player.
     * @param user The UUID of the player.
     * @param limit The number of transactions to get.
     * @param skip The number of transactions to skip.
     * @return A list of deleted transactions for the player.
     */
    List<CurrencyTransaction> getDeletedTransactions(UUID user, int limit, int skip);

    /**
     * Get the deleted transactions of a player.
     * @param user The UUID of the player.
     * @return A list of deleted transactions for the player.
     */
    default List<CurrencyTransaction> getDeletedTransactions(UUID user) {
        return getDeletedTransactions(user, 0, 0);
    }

    /**
     * Get a transaction by its ID.
     * @param transactionId The ID of the transaction.
     * @return The transaction object.
     */
    CurrencyTransaction getTransaction(UUID transactionId);

    /**
     * Get a deleted transaction by its ID.
     * @param transactionId The ID of the transaction.
     * @return The transaction object.
     */
    CurrencyTransaction getDeletedTransaction(UUID transactionId);

    /**
     * Get the total number of transactions for a player.
     * @param user The UUID of the player.
     * @return The total number of transactions.
     */
    long transactionsCount(UUID user);

    /**
     * Get the total number of deleted transactions for a player.
     * @param user The UUID of the player.
     * @return The total number of deleted transactions.
     */
    long deletedTransactionsCount(UUID user);

    /**
     * This will recount all transactions to recalculate the balannce of this user, this can
     * @param user The UUID of the player.
     * @return A list of UUIDs of players that were recalculated.
     */
    List<UUID> recalculateBalance(UUID user);

    /**
     * Get the transaction history of a player.
     * @param user The UUID of the player.
     * @return A list of transactions for the player.
     */
    List<CurrencyTransaction> history(UUID user);

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

    /**
     * Get the amount of currency users.
     * @return The amount of currency users.
     */
    long currencyUserCount();

    /**
     * Run a transaction
     * @param transactionRunnable The transaction to run.
     * @return A CompletableFuture that will be completed when the transaction is done, true if the transaction was successful, false if it failed.
     */
    CompletableFuture<Boolean> transaction(Runnable transactionRunnable);

    /**
     * Helper function to get a currency user from the cache.
     * @param target The UUID of the target user.
     * @param cacheUser The consumer to run with the currency user.
     */
    default void forCacheUser(UUID target, Consumer<CurrencyUser> cacheUser) {
        Optional<CurrencyUser> localUser = CurrencyApi.getService().getCachedUser(target);
        if (localUser.isPresent()) {
            cacheUser.accept(localUser.get());
            return;
        }
    }

    /**
     * Run a transaction and update the cache for the given user ids from the database.
     * @param transactionRunnable The transaction to run.
     * @param userIds The UUIDs of the users to update the cache for.
     * @return A CompletableFuture that will be completed when the transaction is done, true if the transaction was successful, false if it failed.
     */
    default CompletableFuture<Boolean> transactionAndUpdateCacheFromDB(Runnable transactionRunnable, UUID... userIds) {
        return transaction(transactionRunnable).thenApplyAsync(success -> {
            if (!success) {
                return false;
            }

            for (UUID userId : userIds) {
                CurrencyUser user = CurrencyApi.getService().getUserFromDatabase(userId).join();
                if (user != null) {
                    CurrencyApi.getService().addCachedUser(user);
                }
            }
            return true;
        });
    }
}