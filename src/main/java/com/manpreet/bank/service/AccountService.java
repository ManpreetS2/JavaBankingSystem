package com.manpreet.bank.service;

import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.exception.AccountAccessException;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.exception.EntityNotFoundException;
import com.manpreet.bank.exception.InsufficientFundsException;
import com.manpreet.bank.exception.ValidationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.repository.AccountRepository;
import com.manpreet.bank.repository.TransactionRepository;
import com.manpreet.bank.util.MoneyUtil;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class AccountService {

    private final DatabaseManager databaseManager;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(DatabaseManager databaseManager,
                          AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.transactionRepository = Objects.requireNonNull(transactionRepository);
    }

    public List<Account> getAccountsForUser(long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Account getAccount(long userId, long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account was not found"));
        ensureOwnership(userId, account);
        return account;
    }

    public BigDecimal getTotalBalance(long userId) {
        return getAccountsForUser(userId).stream()
                .map(Account::getBalance)
                .reduce(MoneyUtil.ZERO, BigDecimal::add);
    }

    public DepositResult deposit(long userId, long accountId, BigDecimal amount, String description) {
        BigDecimal depositAmount = MoneyUtil.requirePositiveAmount(amount, "Deposit amount");
        String normalizedDescription = normalizeDescription(description, "Deposit");

        try {
            return databaseManager.executeInTransaction(connection -> {
                Account account = requireOwnedAccount(connection, userId, accountId);
                BigDecimal newBalance = account.getBalance().add(depositAmount);
                Account updated = accountRepository.updateBalance(connection, account.getId(), newBalance);

                Transaction transaction = new Transaction(
                        0L,
                        updated.getId(),
                        null,
                        TransactionType.DEPOSIT,
                        depositAmount,
                        normalizedDescription,
                        LocalDateTime.now()
                );
                transactionRepository.create(connection, transaction);
                return new DepositResult(updated, transaction);
            });
        } catch (AccountAccessException | EntityNotFoundException | ValidationException | InsufficientFundsException e) {
            throw e;
        } catch (SQLException e) {
            throw new BankingOperationException("Deposit failed. Please try again.", e);
        }
    }

    public WithdrawalResult withdraw(long userId, long accountId, BigDecimal amount, String description) {
        BigDecimal withdrawalAmount = MoneyUtil.requirePositiveAmount(amount, "Withdrawal amount");
        String normalizedDescription = normalizeDescription(description, "Withdrawal");

        try {
            return databaseManager.executeInTransaction(connection -> {
                Account account = requireOwnedAccount(connection, userId, accountId);
                if (MoneyUtil.compare(account.getBalance(), withdrawalAmount) < 0) {
                    throw new InsufficientFundsException("Insufficient funds for this withdrawal");
                }

                BigDecimal newBalance = account.getBalance().subtract(withdrawalAmount);
                Account updated = accountRepository.updateBalance(connection, account.getId(), newBalance);

                Transaction transaction = new Transaction(
                        0L,
                        updated.getId(),
                        null,
                        TransactionType.WITHDRAWAL,
                        withdrawalAmount,
                        normalizedDescription,
                        LocalDateTime.now()
                );
                transactionRepository.create(connection, transaction);
                return new WithdrawalResult(updated, transaction);
            });
        } catch (AccountAccessException | EntityNotFoundException | ValidationException | InsufficientFundsException e) {
            throw e;
        } catch (SQLException e) {
            throw new BankingOperationException("Withdrawal failed. Please try again.", e);
        }
    }

    public TransferResult transfer(long userId,
                                   long sourceAccountId,
                                   long destinationAccountId,
                                   BigDecimal amount,
                                   String description) {
        if (sourceAccountId == destinationAccountId) {
            throw new ValidationException("Source and destination accounts must be different");
        }

        BigDecimal transferAmount = MoneyUtil.requirePositiveAmount(amount, "Transfer amount");
        String normalizedDescription = normalizeDescription(description, "Transfer");

        try {
            return databaseManager.executeInTransaction(connection -> {
                Account source = requireOwnedAccount(connection, userId, sourceAccountId);
                Account destination = requireOwnedAccount(connection, userId, destinationAccountId);

                if (MoneyUtil.compare(source.getBalance(), transferAmount) < 0) {
                    throw new InsufficientFundsException("Insufficient funds for this transfer");
                }

                BigDecimal newSourceBalance = source.getBalance().subtract(transferAmount);
                BigDecimal newDestinationBalance = destination.getBalance().add(transferAmount);

                Account updatedSource = accountRepository.updateBalance(connection, source.getId(), newSourceBalance);
                Account updatedDestination = accountRepository.updateBalance(
                        connection, destination.getId(), newDestinationBalance
                );

                LocalDateTime createdAt = LocalDateTime.now();

                Transaction outgoing = new Transaction(
                        0L,
                        updatedSource.getId(),
                        updatedDestination.getId(),
                        TransactionType.TRANSFER_OUT,
                        transferAmount,
                        normalizedDescription,
                        createdAt
                );
                transactionRepository.create(connection, outgoing);

                Transaction incoming = new Transaction(
                        0L,
                        updatedDestination.getId(),
                        updatedSource.getId(),
                        TransactionType.TRANSFER_IN,
                        transferAmount,
                        normalizedDescription,
                        createdAt
                );
                transactionRepository.create(connection, incoming);

                return new TransferResult(updatedSource, updatedDestination, outgoing, incoming);
            });
        } catch (AccountAccessException | EntityNotFoundException | ValidationException | InsufficientFundsException e) {
            throw e;
        } catch (SQLException e) {
            throw new BankingOperationException("Transfer failed. Please try again.", e);
        }
    }

    private Account requireOwnedAccount(java.sql.Connection connection, long userId, long accountId)
            throws SQLException {
        Account account = accountRepository.findById(connection, accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account was not found"));
        ensureOwnership(userId, account);
        return account;
    }

    private static void ensureOwnership(long userId, Account account) {
        if (account.getUserId() != userId) {
            throw new AccountAccessException("You do not have access to this account");
        }
    }

    private static String normalizeDescription(String description, String defaultValue) {
        if (description == null || description.isBlank()) {
            return defaultValue;
        }
        String trimmed = description.trim();
        if (trimmed.length() > 255) {
            throw new ValidationException("Description must be at most 255 characters");
        }
        return trimmed;
    }
}
