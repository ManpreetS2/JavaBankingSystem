package com.manpreet.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationPathsTest {

    @TempDir
    Path tempHome;

    @Test
    void macOsUsesApplicationSupport() {
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Mac OS X"),
                env(),
                tempHome
        );
        assertEquals(
                tempHome.resolve("Library").resolve("Application Support").resolve(AppInfo.DATA_DIRECTORY_NAME),
                resolved
        );
    }

    @Test
    void windowsUsesAppDataWhenPresent() {
        Path appData = tempHome.resolve("RoamingAppData");
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Windows 11"),
                env("APPDATA", appData.toString()),
                tempHome
        );
        assertEquals(appData.resolve(AppInfo.DATA_DIRECTORY_NAME), resolved);
    }

    @Test
    void linuxPrefersXdgDataHome() {
        Path xdg = tempHome.resolve("xdg-data");
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Linux"),
                env("XDG_DATA_HOME", xdg.toString()),
                tempHome
        );
        assertEquals(xdg.resolve(AppInfo.DATA_DIRECTORY_NAME), resolved);
    }

    @Test
    void linuxFallsBackToLocalShare() {
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Linux"),
                env(),
                tempHome
        );
        assertEquals(
                tempHome.resolve(".local").resolve("share").resolve(AppInfo.DATA_DIRECTORY_NAME),
                resolved
        );
    }

    @Test
    void systemPropertyOverridesApplicationDataPath() {
        Path override = tempHome.resolve("custom").resolve("demo.db");
        Path resolved = ApplicationPaths.resolveDatabasePath(
                props("os.name", "Linux", ApplicationPaths.DB_PATH_PROPERTY, override.toString()),
                env(),
                tempHome
        );
        assertEquals(override.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void defaultDatabasePathUsesApplicationDataDirectory() {
        Path resolved = ApplicationPaths.resolveDatabasePath(
                props("os.name", "Linux"),
                env(),
                tempHome
        );
        Path expected = tempHome.resolve(".local")
                .resolve("share")
                .resolve(AppInfo.DATA_DIRECTORY_NAME)
                .resolve(ApplicationPaths.DATABASE_FILE_NAME)
                .toAbsolutePath()
                .normalize();
        assertEquals(expected, resolved);
        assertTrue(resolved.endsWith(ApplicationPaths.DATABASE_FILE_NAME));
    }

    private static Function<String, String> props(String... keyValues) {
        Map<String, String> map = toMap(keyValues);
        return map::get;
    }

    private static Function<String, String> env(String... keyValues) {
        Map<String, String> map = toMap(keyValues);
        return map::get;
    }

    private static Map<String, String> toMap(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("key/value pairs required");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
