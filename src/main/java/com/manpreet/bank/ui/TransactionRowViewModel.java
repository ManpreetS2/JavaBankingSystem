package com.manpreet.bank.ui;

/**
 * Immutable UI-facing transaction row used by Dashboard, Accounts, and Transactions.
 */
public record TransactionRowViewModel(
        long id,
        String typeLabel,
        String accountLabel,
        String description,
        String dateLabel,
        String signedAmount,
        boolean credit,
        String relatedAccountLabel
) {
}
