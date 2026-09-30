package com.manpreet.bank;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves stable application-data locations for the desktop SQLite database.
 *
 * <p>Resolution order for the database file:
 * <ol>
 *   <li>explicit {@code -Dbank.db.path=/absolute/or/relative/path.db}</li>
 *   <li>OS application-data directory / {@code banking.db}</li>
 * </ol>
 *
 * <p>Tests and tooling should continue to inject an explicit {@link Path} into
 * {@link AppContext} / {@link com.manpreet.bank.database.DatabaseManager}.
 */
public final class ApplicationPaths {

    public static final String DB_PATH_PROPERTY = "bank.db.path";
    public static final String DATABASE_FILE_NAME = "banking.db";

    private ApplicationPaths() {
    }

    public static Path resolveDatabasePath() {
        return resolveDatabasePath(
                System::getProperty,
                System::getenv,
                Path.of(System.getProperty("user.home"))
        );
    }

    static Path resolveDatabasePath(Function<String, String> properties,
                                    Function<String, String> environment,
                                    Path userHome) {
        Objects.requireNonNull(properties, "properties must not be null");
        Objects.requireNonNull(environment, "environment must not be null");
        Objects.requireNonNull(userHome, "userHome must not be null");

        String override = properties.apply(DB_PATH_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim()).toAbsolutePath().normalize();
        }
        return applicationDataDirectory(properties, environment, userHome)
                .resolve(DATABASE_FILE_NAME)
                .toAbsolutePath()
                .normalize();
    }

    public static Path applicationDataDirectory() {
        return applicationDataDirectory(
                System::getProperty,
                System::getenv,
                Path.of(System.getProperty("user.home"))
        );
    }

    static Path applicationDataDirectory(Function<String, String> properties,
                                         Function<String, String> environment,
                                         Path userHome) {
        Objects.requireNonNull(properties, "properties must not be null");
        Objects.requireNonNull(environment, "environment must not be null");
        Objects.requireNonNull(userHome, "userHome must not be null");

        String os = valueOrEmpty(properties.apply("os.name")).toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return userHome.resolve("Library")
                    .resolve("Application Support")
                    .resolve(AppInfo.DATA_DIRECTORY_NAME);
        }
        if (os.contains("win")) {
            String appData = environment.apply("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Path.of(appData).resolve(AppInfo.DATA_DIRECTORY_NAME);
            }
            return userHome.resolve("AppData")
                    .resolve("Roaming")
                    .resolve(AppInfo.DATA_DIRECTORY_NAME);
        }

        String xdg = environment.apply("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg).resolve(AppInfo.DATA_DIRECTORY_NAME);
        }
        return userHome.resolve(".local")
                .resolve("share")
                .resolve(AppInfo.DATA_DIRECTORY_NAME);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
