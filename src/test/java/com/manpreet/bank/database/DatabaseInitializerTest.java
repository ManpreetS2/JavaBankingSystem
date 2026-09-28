package com.manpreet.bank.database;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    void initializeCreatesRequiredTables() throws Exception {
        Path databaseFile = tempDir.resolve("test-banking.db");
        DatabaseManager databaseManager = new DatabaseManager(databaseFile);
        DatabaseInitializer initializer = new DatabaseInitializer(databaseManager);

        initializer.initialize();

        assertTrue(Files.exists(databaseFile));

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
        Path databaseFile = tempDir.resolve("idempotent-banking.db");
        DatabaseManager databaseManager = new DatabaseManager(databaseFile);
        DatabaseInitializer initializer = new DatabaseInitializer(databaseManager);

        initializer.initialize();
        initializer.initialize();

        assertTrue(Files.exists(databaseFile));
    }
}
