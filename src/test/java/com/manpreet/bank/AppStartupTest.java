package com.manpreet.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.database.DatabaseInitializer;
import com.manpreet.bank.exception.AuthenticationException;
import com.manpreet.bank.service.DemoDataSeeder;
import com.manpreet.bank.service.TransactionFilter;
import com.manpreet.bank.support.TestAppContext;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppStartupTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearProperties() {
        System.clearProperty(AppStartup.DEMO_SEED_PROPERTY);
        System.clearProperty(ApplicationPaths.DB_PATH_PROPERTY);
    }

    @Test
    void demoSeedDisabledByDefault() {
        System.clearProperty(AppStartup.DEMO_SEED_PROPERTY);
        assertFalse(AppStartup.isDemoSeedEnabled());
    }

    @Test
    void demoSeedEnabledOnlyForTrue() {
        System.setProperty(AppStartup.DEMO_SEED_PROPERTY, "true");
        assertTrue(AppStartup.isDemoSeedEnabled());

        System.setProperty(AppStartup.DEMO_SEED_PROPERTY, "TRUE");
        assertTrue(AppStartup.isDemoSeedEnabled());

        System.setProperty(AppStartup.DEMO_SEED_PROPERTY, "false");
        assertFalse(AppStartup.isDemoSeedEnabled());

        System.setProperty(AppStartup.DEMO_SEED_PROPERTY, "yes");
        assertFalse(AppStartup.isDemoSeedEnabled());
    }

    @Test
    void initializeWithoutDemoDoesNotCreateDemoUser() {
        System.clearProperty(AppStartup.DEMO_SEED_PROPERTY);
        AppContext context = TestAppContext.create(tempDir.resolve("prod.db"));
        AppStartup.initialize(context);
        assertFalse(demoUserExists(context));
    }

    @Test
    void initializeWithDemoSeedsOnceAndIsIdempotent() {
        System.setProperty(AppStartup.DEMO_SEED_PROPERTY, "true");
        Path db = tempDir.resolve("demo.db");

        AppContext first = TestAppContext.create(db);
        AppStartup.initialize(first);
        assertTrue(demoUserExists(first));

        long userId = first.getAuthService()
                .authenticate(DemoDataSeeder.DEMO_USERNAME, DemoDataSeeder.DEMO_PASSWORD)
                .userId();
        int txnCount = first.getTransactionService()
                .search(userId, new TransactionFilter(null, null, null, null, null, 100, 0))
                .size();

        AppContext second = TestAppContext.create(db);
        AppStartup.initialize(second);
        assertEquals(
                userId,
                second.getAuthService()
                        .authenticate(DemoDataSeeder.DEMO_USERNAME, DemoDataSeeder.DEMO_PASSWORD)
                        .userId()
        );
        assertEquals(
                txnCount,
                second.getTransactionService()
                        .search(userId, new TransactionFilter(null, null, null, null, null, 100, 0))
                        .size()
        );
    }

    @Test
    void startupSummaryIncludesVersionAndSchema() {
        AppContext context = TestAppContext.create(tempDir.resolve("summary.db"));
        String summary = AppStartup.startupSummary(context);
        assertTrue(summary.contains(AppInfo.VERSION));
        assertTrue(summary.contains(AppInfo.APPLICATION_NAME));
        assertTrue(summary.contains(String.valueOf(DatabaseInitializer.CURRENT_SCHEMA_VERSION)));
        assertTrue(summary.contains("Database:"));
    }

    @Test
    void safeStartupFailureUsesActionableMessage() {
        assertEquals(
                "Unable to create database directory: /nope",
                AppStartup.safeStartupFailureMessage(
                        new IllegalStateException("Unable to create database directory: /nope")
                )
        );
        assertTrue(AppStartup.safeStartupFailureMessage(new RuntimeException())
                .contains(AppInfo.APPLICATION_NAME));
    }

    @Test
    void resolveDatabasePathHonorsSystemProperty() {
        Path override = tempDir.resolve("override.db");
        System.setProperty(ApplicationPaths.DB_PATH_PROPERTY, override.toString());
        assertEquals(override.toAbsolutePath().normalize(), AppStartup.resolveDatabasePath());
    }

    private static boolean demoUserExists(AppContext context) {
        try {
            context.getAuthService().authenticate(
                    DemoDataSeeder.DEMO_USERNAME,
                    DemoDataSeeder.DEMO_PASSWORD
            );
            return true;
        } catch (AuthenticationException e) {
            return false;
        }
    }
}
