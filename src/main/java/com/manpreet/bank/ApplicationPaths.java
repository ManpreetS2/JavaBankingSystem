package com.manpreet.bank;

import java.nio.file.InvalidPathException;
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
            Path appData = absolutePathOrNull(environment.apply("APPDATA"));
            if (appData != null) {
                return appData.resolve(AppInfo.DATA_DIRECTORY_NAME);
            }
            return userHome.resolve("AppData")
                    .resolve("Roaming")
                    .resolve(AppInfo.DATA_DIRECTORY_NAME);
        }

        // The XDG Base Directory specification says relative values are invalid and must be ignored.
        Path xdg = absolutePathOrNull(environment.apply("XDG_DATA_HOME"));
        if (xdg != null) {
            return xdg.resolve(AppInfo.DATA_DIRECTORY_NAME);
        }
        return userHome.resolve(".local")
                .resolve("share")
                .resolve(AppInfo.DATA_DIRECTORY_NAME);
    }

    /**
     * Returns the value as a path when it is an absolute path; blank, relative, or malformed values return null
     * so a relative environment value cannot place the database under the working directory.
     */
    private static Path absolutePathOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(value.trim());
            return path.isAbsolute() ? path : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
