package com.manpreet.bank.repository;

import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.exception.BankingOperationException;
import com.manpreet.bank.model.Transaction;
import com.manpreet.bank.model.TransactionType;
import com.manpreet.bank.service.TransactionFilter;
import com.manpreet.bank.util.MoneyUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
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
        return findByFilter(userId, TransactionFilter.recent(limit));
    }

    public List<Transaction> findRecentByUserId(Connection connection, long userId, int limit) throws SQLException {
        return findByFilter(connection, userId, TransactionFilter.recent(limit));
    }

    public List<Transaction> findByUserId(long userId) {
        return findByFilter(userId, new TransactionFilter(null, null, null, null, null, 100, 0));
    }

    public List<Transaction> findByFilter(long userId, TransactionFilter filter) {
        try (Connection connection = databaseManager.getConnection()) {
            return findByFilter(connection, userId, filter);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to load filtered transactions", e);
        }
    }

    public List<Transaction> findByFilter(Connection connection, long userId, TransactionFilter filter)
            throws SQLException {
        Objects.requireNonNull(filter, "filter must not be null");
        TransactionFilter resolved = filter.withDefaults(20);

        StringBuilder sql = new StringBuilder("""
                SELECT t.id, t.account_id, t.related_account_id, t.transaction_type,
                       t.amount, t.description, t.created_at
                FROM transactions t
                INNER JOIN accounts a ON a.id = t.account_id
                WHERE a.user_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(userId);
        appendFilterClauses(sql, params, resolved);

        sql.append(" ORDER BY t.created_at DESC, t.id DESC LIMIT ? OFFSET ?");
        params.add(resolved.limit());
        params.add(resolved.offset());

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Transaction> transactions = new ArrayList<>();
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
                return transactions;
            }
        }
    }

    public long countByFilter(long userId, TransactionFilter filter) {
        try (Connection connection = databaseManager.getConnection()) {
            return countByFilter(connection, userId, filter);
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to count transactions", e);
        }
    }

    public long countByFilter(Connection connection, long userId, TransactionFilter filter) throws SQLException {
        TransactionFilter resolved = Objects.requireNonNull(filter).withDefaults(20);
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM transactions t
                INNER JOIN accounts a ON a.id = t.account_id
                WHERE a.user_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(userId);
        appendFilterClauses(sql, params, resolved);

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public BigDecimal sumAmountByType(long userId, TransactionType type, LocalDate startDate, LocalDate endDate) {
        try (Connection connection = databaseManager.getConnection()) {
            StringBuilder sql = new StringBuilder("""
                    SELECT t.amount
                    FROM transactions t
                    INNER JOIN accounts a ON a.id = t.account_id
                    WHERE a.user_id = ?
                      AND t.transaction_type = ?
                    """);
            List<Object> params = new ArrayList<>();
            params.add(userId);
            params.add(type.name());
            TransactionFilter filter = new TransactionFilter(null, type, startDate, endDate, null, 100, 0);
            appendDateClauses(sql, params, filter);

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                bindParams(statement, params);
                BigDecimal total = MoneyUtil.ZERO;
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        total = total.add(MoneyUtil.fromStorageString(resultSet.getString(1)));
                    }
                }
                return total;
            }
        } catch (SQLException e) {
            throw new BankingOperationException("Unable to summarize transaction activity", e);
        }
    }

    private static void appendFilterClauses(StringBuilder sql, List<Object> params, TransactionFilter filter) {
        if (filter.accountId() != null) {
            sql.append(" AND t.account_id = ?");
            params.add(filter.accountId());
        }
        if (filter.transactionType() != null) {
            sql.append(" AND t.transaction_type = ?");
            params.add(filter.transactionType().name());
        }
        appendDateClauses(sql, params, filter);
        if (filter.searchText() != null) {
            sql.append("""
                     AND (
                        LOWER(COALESCE(t.description, '')) LIKE ? ESCAPE '\\'
                        OR LOWER(a.account_number) LIKE ? ESCAPE '\\'
                        OR LOWER(a.account_type) LIKE ? ESCAPE '\\'
                     )
                    """);
            String pattern = "%" + escapeLikeLiteral(filter.searchText().toLowerCase()) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
    }

    /**
     * Treats {@code %}, {@code _}, and {@code \} as literal characters in LIKE patterns.
     */
    public static String escapeLikeLiteral(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static void appendDateClauses(StringBuilder sql, List<Object> params, TransactionFilter filter) {
        if (filter.startDate() != null) {
            sql.append(" AND t.created_at >= ?");
            params.add(filter.startDate().atStartOfDay().toString());
        }
        if (filter.endDate() != null) {
            sql.append(" AND t.created_at < ?");
            params.add(filter.endDate().plusDays(1).atStartOfDay().toString());
        }
    }

    private static void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            if (value instanceof Long longValue) {
                statement.setLong(i + 1, longValue);
            } else if (value instanceof Integer intValue) {
                statement.setInt(i + 1, intValue);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
        }
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
