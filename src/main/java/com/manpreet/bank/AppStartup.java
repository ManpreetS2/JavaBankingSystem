package com.manpreet.bank;

import com.manpreet.bank.database.DatabaseInitializer;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Production startup wiring: database path resolution, initialization, optional demo seeding.
 */
public final class AppStartup {

    public static final String DEMO_SEED_PROPERTY = "bank.demo.seed";

    private AppStartup() {
    }

    public static Path resolveDatabasePath() {
        return ApplicationPaths.resolveDatabasePath();
    }

    public static AppContext createContext() {
        return new AppContext(resolveDatabasePath());
    }

    public static void initialize(AppContext context) {
        Objects.requireNonNull(context, "context must not be null");
        context.initializeDatabase();
        if (isDemoSeedEnabled()) {
            context.getDemoDataSeeder().seedIfAbsent();
        }
    }

    public static boolean isDemoSeedEnabled() {
        String value = System.getProperty(DEMO_SEED_PROPERTY, "false");
        return "true".equalsIgnoreCase(value.trim());
    }

    public static String startupSummary(AppContext context) {
        Objects.requireNonNull(context, "context must not be null");
        Path db = context.getDatabaseManager().getDatabasePath().toAbsolutePath().normalize();
        StringBuilder summary = new StringBuilder();
        summary.append(AppInfo.displayNameWithVersion()).append(System.lineSeparator());
        summary.append("Database: ").append(db).append(System.lineSeparator());
        summary.append("Schema version: ").append(DatabaseInitializer.CURRENT_SCHEMA_VERSION);
        if (isDemoSeedEnabled()) {
            summary.append(System.lineSeparator()).append("Demo seed: enabled");
        }
        return summary.toString();
    }

    public static String safeStartupFailureMessage(Throwable error) {
        if (error == null) {
            return "Unable to start " + AppInfo.APPLICATION_NAME + ".";
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return "Unable to start " + AppInfo.APPLICATION_NAME
                    + ". Check that the application data directory is writable.";
        }
        return message;
    }
}
