package com.wildwoodsmp.currency.api;

public enum CurrencyTransactionType {
    /**
     * The transaction is a payment.
     */
    PAYMENT,

    /**
     * The transaction is a withdrawal.
     */
    WITHDRAWAL,
    /**
     * The transaction is force override.
     */
    OVERRIDE
}
