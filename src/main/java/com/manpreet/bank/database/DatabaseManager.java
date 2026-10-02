package com.manpreet.bank.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import org.sqlite.Function;

/**
 * Manages SQLite JDBC connections for the banking application.
 * Connections enable foreign keys and must be closed by callers (prefer try-with-resources).
 */
public class DatabaseManager {

    /**
     * SQL function that lowercases text with Java's Unicode rules. SQLite's built-in {@code LOWER()}
     * only lowercases ASCII letters, so it cannot match a Java-lowercased search term against text such as "ÉCOLE".
     */
    public static final String UNICODE_LOWER_FUNCTION = "unicode_lower";

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
            registerFunctions(connection);
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

    private static void registerFunctions(Connection connection) throws SQLException {
        Function.create(connection, UNICODE_LOWER_FUNCTION, new UnicodeLower(), 1, Function.FLAG_DETERMINISTIC);
    }

    private static final class UnicodeLower extends Function {

        @Override
        protected void xFunc() throws SQLException {
            String value = value_text(0);
            result(value == null ? null : value.toLowerCase(Locale.ROOT));
        }
    }
}
