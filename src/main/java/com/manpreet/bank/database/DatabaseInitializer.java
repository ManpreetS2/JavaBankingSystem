package com.manpreet.bank.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Creates the required SQLite schema if it does not already exist.
 * Monetary values are stored as TEXT to preserve exact decimal precision.
 */
public class DatabaseInitializer {

    private final DatabaseManager databaseManager;

    public DatabaseInitializer(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager must not be null");
    }

    public void initialize() {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
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
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to initialize the banking database at "
                            + databaseManager.getDatabasePath().toAbsolutePath()
                            + ": " + e.getMessage(),
                    e
            );
        }
    }
}
