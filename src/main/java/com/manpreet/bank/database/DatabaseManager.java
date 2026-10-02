package com.manpreet.bank.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Manages SQLite JDBC connections for the banking application.
 * Connections enable foreign keys and must be closed by callers (prefer try-with-resources).
 */
public class DatabaseManager {

    private final Path databasePath;

    /**
     * Uses the resolved application-data database path.
     * Prefer {@link #DatabaseManager(Path)} in tests.
     */
    public DatabaseManager() {
        this(com.manpreet.bank.ApplicationPaths.resolveDatabasePath());
    }

    public DatabaseManager(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath must not be null");
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    /**
     * Opens a new JDBC connection to the configured SQLite database.
     * Creates the parent data directory when it does not already exist.
     * If connection configuration fails after the connection is opened, the connection is closed
     * before the original {@link SQLException} is rethrown.
     */
    public Connection getConnection() throws SQLException {
        ensureDataDirectory();

        String url = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        Connection connection = DriverManager.getConnection(url);
        try {
            enableForeignKeys(connection);
            return connection;
        } catch (SQLException configurationException) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                configurationException.addSuppressed(closeException);
            }
            throw configurationException;
        }
    }

    /**
     * Executes work on a single connection with auto-commit disabled.
     * Commits on success; rolls back on {@link SQLException} or {@link RuntimeException}.
     */
    public <T> T executeInTransaction(SqlTransactionWork<T> work) throws SQLException {
        Objects.requireNonNull(work, "work must not be null");

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException exception) {
                rollbackPreserving(connection, exception);
                throw exception;
            } catch (RuntimeException exception) {
                rollbackPreserving(connection, exception);
                throw exception;
            }
        }
    }

    private void rollbackPreserving(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private void ensureDataDirectory() throws SQLException {
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new SQLException(
                    "Unable to create the application data directory at " + parent
                            + ". Check that the location is writable.",
                    e
            );
        }
    }

    private void enableForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }
}
