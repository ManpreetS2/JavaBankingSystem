package com.manpreet.bank.util;

import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import java.util.Objects;

/**
 * Presentation helpers for signed amounts and transaction labels.
 * Domain amounts remain positive; signs are display-only.
 */
public final class TransactionPresentation {

    private TransactionPresentation() {
    }

    public static boolean isCredit(TransactionType type) {
        Objects.requireNonNull(type, "type must not be null");
        return type == TransactionType.DEPOSIT || type == TransactionType.TRANSFER_IN;
    }

    public static String signedAmount(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        return CurrencyFormatter.formatSigned(
                transaction.getAmount(),
                isCredit(transaction.getTransactionType())
        );
    }

    public static String typeLabel(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> "Deposit";
            case WITHDRAWAL -> "Withdrawal";
            case TRANSFER_IN -> "Transfer in";
            case TRANSFER_OUT -> "Transfer out";
        };
    }
}
