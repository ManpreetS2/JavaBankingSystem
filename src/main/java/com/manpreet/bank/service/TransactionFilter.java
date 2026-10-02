package com.manpreet.bank.service;

import com.manpreet.bank.model.TransactionType;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Filter criteria for authenticated transaction history queries.
 */
public record TransactionFilter(
        Long accountId,
        TransactionType transactionType,
        LocalDate startDate,
        LocalDate endDate,
        String searchText,
        Integer limit,
        Integer offset
) {

    public static TransactionFilter recent(int limit) {
        return new TransactionFilter(null, null, null, null, null, limit, 0);
    }

    public TransactionFilter withPaging(int limit, int offset) {
        return new TransactionFilter(
                accountId,
                transactionType,
                startDate,
                endDate,
                searchText,
                limit,
                offset
        );
    }

    public TransactionFilter withDefaults(int defaultLimit) {
        int resolvedLimit = limit == null ? defaultLimit : limit;
        int resolvedOffset = offset == null ? 0 : offset;
        String normalizedSearch = searchText == null || searchText.isBlank()
                ? null
                : searchText.trim().toLowerCase(Locale.ROOT);
        return new TransactionFilter(
                accountId,
                transactionType,
                startDate,
                endDate,
                normalizedSearch,
                resolvedLimit,
                resolvedOffset
        );
    }
}
