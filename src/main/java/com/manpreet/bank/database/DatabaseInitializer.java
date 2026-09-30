package com.manpreet.bank.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Creates the required SQLite schema if it does not already exist.
 * Monetary values are stored as TEXT to preserve exact decimal precision.
 *
 * <p>Schema versioning uses SQLite {@code PRAGMA user_version}.
 * Clean databases are initialized at {@link #CURRENT_SCHEMA_VERSION}.
 * Unversioned legacy development databases are rejected with an actionable message
 * rather than being silently relabeled. Newer unsupported versions are also rejected.
 */
public class DatabaseInitializer {

    public static final int CURRENT_SCHEMA_VERSION = 2;

    private static final String LEGACY_SCHEMA_MESSAGE =
            "The local database uses an older schema. "
                    + "Delete the database file shown in the startup log and restart the application to recreate it.";

    private final DatabaseManager databaseManager;

    public DatabaseInitializer(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public void initialize() {
        try (Connection connection = databaseManager.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                int currentVersion = readUserVersion(statement);

                if (currentVersion > CURRENT_SCHEMA_VERSION) {
                    throw new IllegalStateException(
                            "Unsupported database schema version " + currentVersion
                                    + "; this application supports version " + CURRENT_SCHEMA_VERSION + "."
                    );
                }

                boolean tablesExist = bankingTablesExist(statement);

                if (currentVersion == 0 && !tablesExist) {
                    createBaseSchema(statement);
                    ensureIndexes(statement);
                    setUserVersion(statement, CURRENT_SCHEMA_VERSION);
                    connection.commit();
                    return;
                }

                if (currentVersion == 0 && tablesExist) {
                    throw new IllegalStateException(LEGACY_SCHEMA_MESSAGE);
                }

                if (currentVersion == CURRENT_SCHEMA_VERSION) {
                    ensureIndexes(statement);
                    connection.commit();
                    return;
                }

                // Known older versioned schema without an automated migration path yet.
                throw new IllegalStateException(
                        "The local database uses schema version " + currentVersion
                                + " but this application requires version " + CURRENT_SCHEMA_VERSION
                                + ". Delete the database file shown in the startup log and restart the application."
                );
            } catch (RuntimeException | SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                if (exception instanceof IllegalStateException illegalStateException) {
                    throw illegalStateException;
                }
                throw new IllegalStateException(
                        "Failed to initialize the banking database at "
                                + databaseManager.getDatabasePath().toAbsolutePath()
                                + ": " + exception.getMessage(),
                        exception
                );
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // Connection is closing; auto-commit restoration is best-effort.
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to initialize the banking database at "
                            + databaseManager.getDatabasePath().toAbsolutePath()
                            + ": " + e.getMessage(),
                    e
            );
        }
    }

    public int readSchemaVersion() {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            return readUserVersion(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read schema version", e);
        }
    }

    private static boolean bankingTablesExist(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'table'
                  AND name IN ('users', 'accounts', 'transactions')
                """
        )) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }

    private void createBaseSchema(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    account_number TEXT NOT NULL UNIQUE,
                    account_type TEXT NOT NULL
                        CHECK (account_type IN ('CHECKING', 'SAVINGS')),
                    balance TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_id INTEGER NOT NULL,
                    related_account_id INTEGER,
                    transaction_type TEXT NOT NULL
                        CHECK (transaction_type IN (
                            'DEPOSIT',
                            'WITHDRAWAL',
                            'TRANSFER_IN',
                            'TRANSFER_OUT'
                        )),
                    amount TEXT NOT NULL,
                    description TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (account_id) REFERENCES accounts(id),
                    FOREIGN KEY (related_account_id) REFERENCES accounts(id)
                )
                """);
    }

    private void ensureIndexes(Statement statement) throws SQLException {
        statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_accounts_user_id
                ON accounts(user_id)
                """);

        statement.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_user_id_account_type
                ON accounts(user_id, account_type)
                """);

        statement.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_nocase
                ON users(username COLLATE NOCASE)
                """);

        statement.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_nocase
                ON users(email COLLATE NOCASE)
                """);

        statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_account_id
                ON transactions(account_id)
                """);

        statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_created_at
                ON transactions(created_at)
                """);

        statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_transactions_account_created
                ON transactions(account_id, created_at)
                """);
    }

    private static int readUserVersion(Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA user_version")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static void setUserVersion(Statement statement, int version) throws SQLException {
        statement.execute("PRAGMA user_version = " + version);
    }
}
