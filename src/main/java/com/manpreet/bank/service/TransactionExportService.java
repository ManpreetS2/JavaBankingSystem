package com.manpreet.bank.service;

import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.util.AccountNumberFormatter;
import com.manpreet.bank.util.TransactionPresentation;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exports filtered transactions to CSV without exposing sensitive fields.
 */
public class TransactionExportService {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;

    public TransactionExportService(TransactionService transactionService,
                                    AccountRepository accountRepository) {
        this.transactionService = Objects.requireNonNull(transactionService);
        this.accountRepository = Objects.requireNonNull(accountRepository);
    }

    public String exportCsv(long userId, TransactionFilter filter) {
        List<Transaction> transactions = transactionService.search(userId, filter);
        Map<Long, Account> accountsById = accountRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Account,Type,Description,Amount\n");
        for (Transaction transaction : transactions) {
            Account account = accountsById.get(transaction.getAccountId());
            String accountLabel = account == null
                    ? "Account"
                    : AccountNumberFormatter.displayLabel(account.getAccountType(), account.getAccountNumber());
            csv.append(escape(transaction.getCreatedAt().toString())).append(',')
                    .append(escape(accountLabel)).append(',')
                    .append(escape(TransactionPresentation.typeLabel(transaction.getTransactionType()))).append(',')
                    .append(escape(transaction.getDescription() == null ? "" : transaction.getDescription())).append(',')
                    .append(escape(TransactionPresentation.signedAmount(transaction)))
                    .append('\n');
        }
        return csv.toString();
    }

    public byte[] exportCsvBytes(long userId, TransactionFilter filter) {
        return exportCsv(userId, filter).getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        boolean needsQuotes = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        String escaped = safe.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
