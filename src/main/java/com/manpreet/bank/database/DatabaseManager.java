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

    private static final Path DEFAULT_DATABASE_PATH = Path.of("data", "banking.db");

    private final Path databasePath;

    public DatabaseManager() {
        this(DEFAULT_DATABASE_PATH);
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

    private void ensureDataDirectory() throws SQLException {
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new SQLException("Unable to create database directory: " + parent, e);
        }
    }

    private void enableForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }
}
