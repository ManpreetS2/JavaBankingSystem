package com.manpreet.bank.support;

import com.manpreet.bank.AppContext;
import com.manpreet.bank.database.DatabaseManager;
import com.manpreet.bank.util.PasswordHasher;
import java.nio.file.Path;

/**
 * Shared test wiring. Uses a reduced PBKDF2 iteration count for speed while
 * production {@link PasswordHasher} remains at 600,000 iterations.
 */
public final class TestAppContext {

    public static final int TEST_PBKDF2_ITERATIONS = 10_000;

    private TestAppContext() {
    }

    public static AppContext create(Path databaseFile) {
        DatabaseManager databaseManager = new DatabaseManager(databaseFile);
        AppContext context = new AppContext(databaseManager, new PasswordHasher(TEST_PBKDF2_ITERATIONS));
        context.initializeDatabase();
        return context;
    }
}
