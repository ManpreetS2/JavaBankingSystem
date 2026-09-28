package com.manpreet.bank.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseInitializerTest {

    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private DatabaseInitializer initializer;

    @BeforeEach
    void setUp() {
        Path databaseFile = tempDir.resolve("test-banking.db");
        databaseManager = new DatabaseManager(databaseFile);
        initializer = new DatabaseInitializer(databaseManager);
        initializer.initialize();
    }

    @Test
    void initializeCreatesRequiredTables() throws Exception {
        assertTrue(Files.exists(databaseManager.getDatabasePath()));

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     """
                     SELECT name FROM sqlite_master
                     WHERE type = 'table'
                       AND name IN ('users', 'accounts', 'transactions')
                     """
             )) {
            Set<String> tableNames = new HashSet<>();
            while (resultSet.next()) {
                tableNames.add(resultSet.getString("name"));
            }

            assertTrue(tableNames.contains("users"));
            assertTrue(tableNames.contains("accounts"));
            assertTrue(tableNames.contains("transactions"));
        }
    }

    @Test
    void initializeIsIdempotent() {
        initializer.initialize();
        initializer.initialize();

        assertTrue(Files.exists(databaseManager.getDatabasePath()));
    }

    @Test
    void getConnectionEnablesForeignKeys() throws Exception {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }

    @Test
    void rejectsAccountWithMissingUserForeignKey() {
        SQLException exception = assertThrows(SQLException.class, () -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         """
                         INSERT INTO accounts (user_id, account_number, account_type, balance, created_at)
                         VALUES (?, ?, ?, ?, ?)
                         """
                 )) {
                statement.setLong(1, 999L);
                statement.setString(2, "CHK-MISSING-USER");
                statement.setString(3, "CHECKING");
                statement.setString(4, "100.00");
                statement.setString(5, "2026-09-27T12:00:00");
                statement.executeUpdate();
            }
        });

        assertTrue(exception.getMessage().toLowerCase().contains("foreign key"));
    }

    @Test
    void rejectsInvalidRelatedAccountForeignKey() throws Exception {
        long userId = insertUser("fk-user", "fk@example.com");
        long accountId = insertAccount(userId, "CHK-10001", "CHECKING", "50.00");

        SQLException exception = assertThrows(SQLException.class, () -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         """
                         INSERT INTO transactions (
                             account_id, related_account_id, transaction_type, amount, description, created_at
                         ) VALUES (?, ?, ?, ?, ?, ?)
                         """
                 )) {
                statement.setLong(1, accountId);
                statement.setLong(2, 999L);
                statement.setString(3, "TRANSFER_OUT");
                statement.setString(4, "10.00");
                statement.setString(5, "Invalid related account");
                statement.setString(6, "2026-09-27T12:00:00");
                statement.executeUpdate();
            }
        });

        assertTrue(exception.getMessage().toLowerCase().contains("foreign key"));
    }

    @Test
    void allowsNullableRelatedAccountId() throws Exception {
        long userId = insertUser("nullable-user", "nullable@example.com");
        long accountId = insertAccount(userId, "CHK-10002", "CHECKING", "50.00");

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     """
                     INSERT INTO transactions (
                         account_id, related_account_id, transaction_type, amount, description, created_at
                     ) VALUES (?, NULL, ?, ?, ?, ?)
                     """
             )) {
            statement.setLong(1, accountId);
            statement.setString(2, "DEPOSIT");
            statement.setString(3, "25.00");
            statement.setString(4, "Cash deposit");
            statement.setString(5, "2026-09-27T12:00:00");
            assertEquals(1, statement.executeUpdate());
        }
    }

    @Test
    void rejectsInvalidAccountType() throws Exception {
        long userId = insertUser("check-user", "check@example.com");

        SQLException exception = assertThrows(SQLException.class, () -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         """
                         INSERT INTO accounts (user_id, account_number, account_type, balance, created_at)
                         VALUES (?, ?, ?, ?, ?)
                         """
                 )) {
                statement.setLong(1, userId);
                statement.setString(2, "INV-10001");
                statement.setString(3, "INVESTMENT");
                statement.setString(4, "100.00");
                statement.setString(5, "2026-09-27T12:00:00");
                statement.executeUpdate();
            }
        });

        assertTrue(exception.getMessage().toLowerCase().contains("check"));
    }

    @Test
    void rejectsInvalidTransactionType() throws Exception {
        long userId = insertUser("txn-user", "txn@example.com");
        long accountId = insertAccount(userId, "CHK-10003", "CHECKING", "50.00");

        SQLException exception = assertThrows(SQLException.class, () -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         """
                         INSERT INTO transactions (
                             account_id, related_account_id, transaction_type, amount, description, created_at
                         ) VALUES (?, NULL, ?, ?, ?, ?)
                         """
                 )) {
                statement.setLong(1, accountId);
                statement.setString(2, "MAGIC_TRANSFER");
                statement.setString(3, "10.00");
                statement.setString(4, "Invalid type");
                statement.setString(5, "2026-09-27T12:00:00");
                statement.executeUpdate();
            }
        });

        assertTrue(exception.getMessage().toLowerCase().contains("check"));
    }

    @Test
    void persistsExactMonetaryTextWithoutDoubleConversion() throws Exception {
        String exactBalance = "1234567890.123456789";
        long userId = insertUser("money-user", "money@example.com");
        long accountId = insertAccount(userId, "SAV-10001", "SAVINGS", exactBalance);

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT balance FROM accounts WHERE id = ?"
             )) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                String storedBalance = resultSet.getString("balance");
                assertEquals(exactBalance, storedBalance);
                assertEquals(0, new BigDecimal(exactBalance).compareTo(new BigDecimal(storedBalance)));
            }
        }
    }

    @Test
    void initializeSetsCurrentSchemaVersionIdempotently() {
        assertEquals(DatabaseInitializer.CURRENT_SCHEMA_VERSION, initializer.readSchemaVersion());
        initializer.initialize();
        assertEquals(DatabaseInitializer.CURRENT_SCHEMA_VERSION, initializer.readSchemaVersion());
    }

    private long insertUser(String username, String email) throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     """
                     INSERT INTO users (
                         first_name, last_name, email, username, password_hash, created_at
                     ) VALUES (?, ?, ?, ?, ?, ?)
                     """,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setString(1, "Test");
            statement.setString(2, "User");
            statement.setString(3, email);
            statement.setString(4, username);
            statement.setString(5, "hashed-password");
            statement.setString(6, "2026-09-27T12:00:00");
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private long insertAccount(long userId, String accountNumber, String accountType, String balance)
            throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     """
                     INSERT INTO accounts (user_id, account_number, account_type, balance, created_at)
                     VALUES (?, ?, ?, ?, ?)
                     """,
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setLong(1, userId);
            statement.setString(2, accountNumber);
            statement.setString(3, accountType);
            statement.setString(4, balance);
            statement.setString(5, "2026-09-27T12:00:00");
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }
}
