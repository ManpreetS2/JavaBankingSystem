package com.manpreet.bank.repository;

import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.util.MoneyUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TransactionRepository {

    private final DatabaseManager databaseManager;

    public TransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public Transaction create(Transaction transaction) {
        try {
            return databaseManager.executeInTransaction(connection -> create(connection, transaction));
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to create transaction", e);
        }
    }

    public Transaction create(Connection connection, Transaction transaction) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(transaction, "transaction must not be null");

        String sql = """
                INSERT INTO transactions (
                    account_id, related_account_id, transaction_type, amount, description, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, transaction.getAccountId());
            if (transaction.getRelatedAccountId() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setLong(2, transaction.getRelatedAccountId());
            }
            statement.setString(3, transaction.getTransactionType().name());
            statement.setString(4, MoneyUtil.toStorageString(transaction.getAmount()));
            statement.setString(5, transaction.getDescription());
            statement.setString(6, formatTimestamp(transaction.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating transaction failed: no generated id");
                }
                transaction.setId(keys.getLong(1));
                transaction.setAmount(MoneyUtil.requireValidAmount(transaction.getAmount(), "amount"));
                return transaction;
            }
        }
    }

    public Optional<Transaction> findById(long id) {
        try (Connection connection = databaseManager.getConnection()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load transaction", e);
        }
    }

    public Optional<Transaction> findById(Connection connection, long id) throws SQLException {
        String sql = """
                SELECT id, account_id, related_account_id, transaction_type, amount, description, created_at
                FROM transactions
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapTransaction(resultSet));
            }
        }
    }

    public List<Transaction> findByAccountId(long accountId) {
        try (Connection connection = databaseManager.getConnection()) {
            return findByAccountId(connection, accountId);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load transactions for account", e);
        }
    }

    public List<Transaction> findByAccountId(Connection connection, long accountId) throws SQLException {
        String sql = """
                SELECT id, account_id, related_account_id, transaction_type, amount, description, created_at
                FROM transactions
                WHERE account_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Transaction> transactions = new ArrayList<>();
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
                return transactions;
            }
        }
    }

    public List<Transaction> findRecentByUserId(long userId, int limit) {
        try (Connection connection = databaseManager.getConnection()) {
            return findRecentByUserId(connection, userId, limit);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load recent transactions for user", e);
        }
    }

    public List<Transaction> findRecentByUserId(Connection connection, long userId, int limit) throws SQLException {
        String sql = """
                SELECT t.id, t.account_id, t.related_account_id, t.transaction_type,
                       t.amount, t.description, t.created_at
                FROM transactions t
                INNER JOIN accounts a ON a.id = t.account_id
                WHERE a.user_id = ?
                ORDER BY t.created_at DESC, t.id DESC
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Transaction> transactions = new ArrayList<>();
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
                return transactions;
            }
        }
    }

    public List<Transaction> findByUserId(long userId) {
        return findRecentByUserId(userId, Integer.MAX_VALUE);
    }

    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        long relatedAccountId = resultSet.getLong("related_account_id");
        Long related = resultSet.wasNull() ? null : relatedAccountId;

        return new Transaction(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                related,
                TransactionType.valueOf(resultSet.getString("transaction_type")),
                MoneyUtil.fromStorageString(resultSet.getString("amount")),
                resultSet.getString("description"),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }

    private static String formatTimestamp(LocalDateTime value) {
        return Objects.requireNonNullElseGet(value, LocalDateTime::now).toString();
    }
}
