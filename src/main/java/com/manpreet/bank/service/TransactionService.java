package com.manpreet.bank.service;

import com.manpreet.bank.exception.AccountAccessException;
import com.manpreet.bank.exception.EntityNotFoundException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class TransactionService {

    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 100;
    public static final int DEFAULT_LIMIT = 20;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    public List<Transaction> getRecentActivity(long userId, int limit) {
        return search(userId, TransactionFilter.recent(limit));
    }

    public List<Transaction> getAccountHistory(long userId, long accountId) {
        return getAccountHistory(userId, accountId, DEFAULT_LIMIT);
    }

    public List<Transaction> getAccountHistory(long userId, long accountId, int limit) {
        ensureOwnedAccount(userId, accountId);
        return search(userId, new TransactionFilter(accountId, null, null, null, null, limit, 0));
    }

    public List<Transaction> search(long userId, TransactionFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        TransactionFilter resolved = validateFilter(userId, filter);
        return transactionRepository.findByFilter(userId, resolved);
    }

    public long count(long userId, TransactionFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        TransactionFilter resolved = validateFilter(userId, filter);
        return transactionRepository.countByFilter(userId, resolved);
    }

    public BigDecimal totalDeposits(long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumAmountByType(userId, TransactionType.DEPOSIT, startDate, endDate);
    }

    public BigDecimal totalWithdrawals(long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumAmountByType(userId, TransactionType.WITHDRAWAL, startDate, endDate);
    }

    public BigDecimal totalTransfersIn(long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumAmountByType(userId, TransactionType.TRANSFER_IN, startDate, endDate);
    }

    public BigDecimal totalTransfersOut(long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumAmountByType(userId, TransactionType.TRANSFER_OUT, startDate, endDate);
    }

    private TransactionFilter validateFilter(long userId, TransactionFilter filter) {
        TransactionFilter resolved = filter.withDefaults(DEFAULT_LIMIT);
        if (resolved.limit() < MIN_LIMIT || resolved.limit() > MAX_LIMIT) {
            throw new ValidationException("Limit must be between " + MIN_LIMIT + " and " + MAX_LIMIT);
        }
        if (resolved.offset() < 0) {
            throw new ValidationException("Offset cannot be negative");
        }
        if (resolved.startDate() != null
                && resolved.endDate() != null
                && resolved.endDate().isBefore(resolved.startDate())) {
            throw new ValidationException("End date cannot be before start date");
        }
        if (resolved.accountId() != null) {
            ensureOwnedAccount(userId, resolved.accountId());
        }
        return resolved;
    }

    private void ensureOwnedAccount(long userId, long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account was not found"));
        if (account.getUserId() != userId) {
            throw new AccountAccessException("You do not have access to this account");
        }
    }
}
