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
                        account_type TEXT NOT NULL,
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
                        transaction_type TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        description TEXT,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (account_id) REFERENCES accounts(id)
                    )
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
