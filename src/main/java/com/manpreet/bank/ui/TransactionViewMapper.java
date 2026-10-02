package com.manpreet.bank.ui;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.util.AccountNumberFormatter;
import com.manpreet.bank.util.DateTimeDisplayFormatter;
import com.manpreet.bank.util.TransactionPresentation;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps persisted transactions into shared UI row models without N+1 lookups.
 */
public final class TransactionViewMapper {

    private TransactionViewMapper() {
    }

    public static List<TransactionRowViewModel> toRows(List<Transaction> transactions,
                                                       Map<Long, Account> accountsById) {
        Objects.requireNonNull(transactions, "transactions must not be null");
        Objects.requireNonNull(accountsById, "accountsById must not be null");
        return transactions.stream()
                .map(transaction -> toRow(transaction, accountsById))
                .toList();
    }

    public static TransactionRowViewModel toRow(Transaction transaction, Map<Long, Account> accountsById) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        Objects.requireNonNull(accountsById, "accountsById must not be null");

        Account account = accountsById.get(transaction.getAccountId());
        Account related = transaction.getRelatedAccountId() == null
                ? null
                : accountsById.get(transaction.getRelatedAccountId());

        String relatedLabel = related == null
                ? null
                : shortAccountLabel(related.getAccountType());

        return new TransactionRowViewModel(
                transaction.getId(),
                displayTitle(transaction.getTransactionType(), relatedLabel),
                account == null
                        ? "Account"
                        : AccountNumberFormatter.displayLabel(account.getAccountType(), account.getAccountNumber()),
                transaction.getDescription() == null ? "" : transaction.getDescription(),
                DateTimeDisplayFormatter.format(transaction.getCreatedAt()),
                TransactionPresentation.signedAmount(transaction),
                TransactionPresentation.isCredit(transaction.getTransactionType()),
                relatedLabel
        );
    }

    public static String displayTitle(TransactionType type, String relatedAccountLabel) {
        return switch (type) {
            case DEPOSIT -> "Deposit";
            case WITHDRAWAL -> "Withdrawal";
            case TRANSFER_OUT -> relatedAccountLabel == null
                    ? "Transfer out"
                    : "Transfer to " + relatedAccountLabel;
            case TRANSFER_IN -> relatedAccountLabel == null
                    ? "Transfer in"
                    : "Transfer from " + relatedAccountLabel;
        };
    }

    private static String shortAccountLabel(AccountType type) {
        return switch (type) {
            case CHECKING -> "Checking";
            case SAVINGS -> "Savings";
        };
    }
}
