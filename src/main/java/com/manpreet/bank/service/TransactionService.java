package com.manpreet.bank.service;

import com.manpreet.bank.exception.AccountAccessException;
import com.manpreet.bank.exception.EntityNotFoundException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.repository.TransactionRepository;
import java.util.List;
import java.util.Objects;

public class TransactionService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    public List<Transaction> getRecentActivity(long userId, int limit) {
        int validatedLimit = validateLimit(limit);
        return transactionRepository.findRecentByUserId(userId, validatedLimit);
    }

    public List<Transaction> getAccountHistory(long userId, long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account was not found"));
        if (account.getUserId() != userId) {
            throw new AccountAccessException("You do not have access to this account");
        }
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> getUserHistory(long userId, int limit) {
        return getRecentActivity(userId, limit);
    }

    private static int validateLimit(int limit) {
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new ValidationException("Limit must be between " + MIN_LIMIT + " and " + MAX_LIMIT);
        }
        return limit;
    }
}
