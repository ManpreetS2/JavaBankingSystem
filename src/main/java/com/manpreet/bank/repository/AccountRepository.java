package com.manpreet.bank.repository;

import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.model.Account;
import com.manpreet.bank.model.AccountType;
import com.manpreet.bank.util.MoneyUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AccountRepository {

    private final DatabaseManager databaseManager;

    public AccountRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public Account create(Account account) {
        try {
            return databaseManager.executeInTransaction(connection -> create(connection, account));
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to create account", e);
        }
    }

    public Account create(Connection connection, Account account) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(account, "account must not be null");

        String sql = """
                INSERT INTO accounts (user_id, account_number, account_type, balance, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, account.getUserId());
            statement.setString(2, account.getAccountNumber());
            statement.setString(3, account.getAccountType().name());
            statement.setString(4, MoneyUtil.toStorageString(account.getBalance()));
            statement.setString(5, formatTimestamp(account.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating account failed: no generated id");
                }
                account.setId(keys.getLong(1));
                account.setBalance(MoneyUtil.requireValidAmount(account.getBalance(), "balance"));
                return account;
            }
        }
    }

    public Optional<Account> findById(long id) {
        try (Connection connection = databaseManager.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load account", e);
        }
    }

    public Optional<Account> findById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT id, user_id, account_number, account_type, balance, created_at
                FROM accounts
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAccount(resultSet));
            }
        }
    }

    public List<Account> findByUserId(long userId) {
        try (Connection connection = databaseManager.getConnection()) {
            return findByUserId(connection, userId);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load accounts for user", e);
        }
    }

    public List<Account> findByUserId(Connection connection, long userId) throws SQLException {
        String sql = """
                SELECT id, user_id, account_number, account_type, balance, created_at
                FROM accounts
                WHERE user_id = ?
                ORDER BY CASE account_type WHEN 'CHECKING' THEN 0 ELSE 1 END, id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (resultSet.next()) {
                    accounts.add(mapAccount(resultSet));
                }
                return accounts;
            }
        }
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        try (Connection connection = databaseManager.getConnection()) {
            return findByAccountNumber(connection, accountNumber);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load account by number", e);
        }
    }

    public Optional<Account> findByAccountNumber(Connection connection, String accountNumber) throws SQLException {
        String sql = """
                SELECT id, user_id, account_number, account_type, balance, created_at
                FROM accounts
                WHERE account_number = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAccount(resultSet));
            }
        }
    }

    public Account updateBalance(long accountId, BigDecimal newBalance) {
        try {
            return databaseManager.executeInTransaction(connection -> updateBalance(connection, accountId, newBalance));
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to update account balance", e);
        }
    }

    public Account updateBalance(Connection connection, long accountId, BigDecimal newBalance) throws SQLException {
        BigDecimal normalized = MoneyUtil.requireValidAmount(newBalance, "balance");
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, MoneyUtil.toStorageString(normalized));
            statement.setLong(2, accountId);
            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Account not found for balance update: " + accountId);
            }
        }
        return findById(connection, accountId)
                .orElseThrow(() -> new SQLException("Account disappeared after balance update: " + accountId));
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        return new Account(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getString("account_number"),
                AccountType.valueOf(resultSet.getString("account_type")),
                MoneyUtil.fromStorageString(resultSet.getString("balance")),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }

    private static String formatTimestamp(LocalDateTime value) {
        return Objects.requireNonNullElseGet(value, LocalDateTime::now).toString();
    }
}
