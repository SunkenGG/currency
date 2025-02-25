package com.wildwoodsmp.currency.api;

import java.util.UUID;

public interface CurrencyUser {
    /**
     * Get the UUID of the user.
     * @return The UUID of the user.
     */
    UUID userId();

    /**
     * Get the balance of the user.
     * @param currency The currency to get the balance of.
     * @return The balance of the user.
     */
    double balance(Currency currency);

    /**
     * Pay a certain amount of currency from one player to another.
     * @param currency The currency to pay.
     * @param amount The amount of currency to pay.
     * @param reason The reason for the payment.
     * @param linkerId An id to link multiple transactions together.
     * @param linkerReason A reason to link multiple transactions together.
     */
    void deposit(Currency currency, double amount, String reason, UUID linkerId, String linkerReason);

    /**
     * Pay a certain amount of currency from one player to another.
     * @param currency The currency to pay.
     * @param amount The amount of currency to pay.
     * @param reason The reason for the payment.
     */
    default void deposit(Currency currency, double amount, String reason) {
        deposit(currency, amount, reason, null, null);
    }

    /**
     * Withdraw a certain amount of currency from the user.
     * @param currency The currency to withdraw.
     * @param amount The amount to withdraw.
     * @param reason The reason for the withdrawal.
     * @param linkerId An id to link multiple transactions together.
     * @param linkerReason A reason to link multiple transactions together.
     */
    void withdraw(Currency currency, double amount, String reason, UUID linkerId, String linkerReason);

    /**
     * Withdraw a certain amount of currency from the user.
     * @param currency The currency to withdraw.
     * @param amount The amount to withdraw.
     * @param reason The reason for the withdrawal.
     */
    default void withdraw(Currency currency, double amount, String reason) {
        withdraw(currency, amount, reason, null, null);
    }

    /**
     * Set the balance of the user.
     * @param currency The currency to set the balance of.
     * @param amount The amount to set the balance to.
     * @param reason The reason for the balance change.
     * @param linkerId An id to link multiple transactions together.
     * @param linkerReason A reason to link multiple transactions together.
     */
    void set(Currency currency, double amount, String reason, UUID linkerId, String linkerReason);

    /**
     * Set the balance of the user.
     * @param currency The currency to set the balance of.
     * @param amount The amount to set the balance to.
     * @param reason The reason for the balance change.
     */
    default void set(Currency currency, double amount, String reason) {
        set(currency, amount, reason, null, null);
    }
}
