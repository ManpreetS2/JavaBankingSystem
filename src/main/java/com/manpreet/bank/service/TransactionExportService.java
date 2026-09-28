package com.manpreet.bank.service;

import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.util.AccountNumberFormatter;
import com.manpreet.bank.util.TransactionPresentation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exports filtered transactions to CSV without exposing sensitive fields.
 *
 * <p>The number of matching rows is counted first. If it exceeds {@link #MAX_EXPORT_ROWS}
 * the export is rejected with a {@link ValidationException}; partial exports are never produced.
 * Matching rows are fetched in batches of {@link #EXPORT_PAGE_SIZE}.
 */
public class TransactionExportService {

    public static final int EXPORT_PAGE_SIZE = 100;
    public static final int MAX_EXPORT_ROWS = 10_000;

    private static final String CSV_HEADER = "Date,Account,Type,Description,Amount\n";

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final int maxExportRows;

    public TransactionExportService(TransactionService transactionService,
                                    AccountRepository accountRepository) {
        this(transactionService, accountRepository, MAX_EXPORT_ROWS);
    }

    TransactionExportService(TransactionService transactionService,
                             AccountRepository accountRepository,
                             int maxExportRows) {
        this.transactionService = Objects.requireNonNull(transactionService);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        if (maxExportRows < 1) {
            throw new IllegalArgumentException("maxExportRows must be positive");
        }
        this.maxExportRows = maxExportRows;
    }

    public String exportCsv(long userId, TransactionFilter filter) {
        List<Transaction> transactions = loadAllMatching(userId, filter);
        Map<Long, Account> accountsById = accountRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER);
        for (Transaction transaction : transactions) {
            Account account = accountsById.get(transaction.getAccountId());
            String accountLabel = account == null
                    ? "Account"
                    : AccountNumberFormatter.displayLabel(account.getAccountType(), account.getAccountNumber());
            String description = transaction.getDescription() == null ? "" : transaction.getDescription();
            csv.append(escape(transaction.getCreatedAt().toString())).append(',')
                    .append(escape(accountLabel)).append(',')
                    .append(escape(TransactionPresentation.typeLabel(transaction.getTransactionType()))).append(',')
                    .append(escape(neutralizeFormula(description))).append(',')
                    .append(escape(TransactionPresentation.signedAmount(transaction)))
                    .append('\n');
        }
        return csv.toString();
    }

    public byte[] exportCsvBytes(long userId, TransactionFilter filter) {
        return exportCsv(userId, filter).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Rejects exports whose matching row count exceeds the supported maximum.
     */
    static void ensureWithinExportLimit(long matchingRows, int maxRows) {
        if (matchingRows > maxRows) {
            throw new ValidationException(limitMessage(maxRows));
        }
    }

    static String limitMessage(int maxRows) {
        return String.format(java.util.Locale.US,
                "Export supports up to %,d transactions. Narrow your filters and try again.", maxRows);
    }

    /**
     * Prevents user-controlled text from being interpreted as a spreadsheet formula by
     * prefixing an apostrophe when the value starts with a formula control character.
     */
    static String neutralizeFormula(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }

    private List<Transaction> loadAllMatching(long userId, TransactionFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        long matching = transactionService.count(userId, pageOf(filter, 0));
        ensureWithinExportLimit(matching, maxExportRows);

        List<Transaction> all = new ArrayList<>((int) matching);
        int offset = 0;
        while (true) {
            List<Transaction> batch = transactionService.search(userId, pageOf(filter, offset));
            all.addAll(batch);
            // Guard against rows inserted between count and fetch.
            ensureWithinExportLimit(all.size(), maxExportRows);
            if (batch.size() < EXPORT_PAGE_SIZE) {
                break;
            }
            offset += EXPORT_PAGE_SIZE;
        }
        return all;
    }

    private static TransactionFilter pageOf(TransactionFilter filter, int offset) {
        return new TransactionFilter(
                filter.accountId(),
                filter.transactionType(),
                filter.startDate(),
                filter.endDate(),
                filter.searchText(),
                EXPORT_PAGE_SIZE,
                offset
        );
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        boolean needsQuotes = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        String escaped = safe.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
