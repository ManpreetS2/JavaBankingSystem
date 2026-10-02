package com.manpreet.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void windowsFallsBackWhenAppDataMissing() {
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Windows 11"),
                env(),
                tempHome
        );
        assertEquals(
                tempHome.resolve("AppData").resolve("Roaming").resolve(AppInfo.DATA_DIRECTORY_NAME),
                resolved
        );
    }

    @Test
    void windowsFallsBackWhenAppDataBlank() {
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Windows 11"),
                env("APPDATA", "   "),
                tempHome
        );
        assertEquals(
                tempHome.resolve("AppData").resolve("Roaming").resolve(AppInfo.DATA_DIRECTORY_NAME),
                resolved
        );
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
    void blankOverrideFallsThroughToApplicationData() {
        Path resolved = ApplicationPaths.resolveDatabasePath(
                props("os.name", "Linux", ApplicationPaths.DB_PATH_PROPERTY, "   "),
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
    }

    @Test
    void relativeOverrideIsNormalizedToAbsolutePath() {
        Path resolved = ApplicationPaths.resolveDatabasePath(
                props("os.name", "Linux", ApplicationPaths.DB_PATH_PROPERTY, "relative-demo.db"),
                env(),
                tempHome
        );
        assertTrue(resolved.isAbsolute());
        assertEquals(Path.of("relative-demo.db").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void overridePathMayContainSpaces() {
        Path override = tempHome.resolve("My Data").resolve("banking demo.db");
        Path resolved = ApplicationPaths.resolveDatabasePath(
                props("os.name", "Linux", ApplicationPaths.DB_PATH_PROPERTY, override.toString()),
                env(),
                tempHome
        );
        assertEquals(override.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void appDataPathWithSpacesIsSupported() {
        Path appData = tempHome.resolve("App Data Folder");
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Windows 11"),
                env("APPDATA", appData.toString()),
                tempHome
        );
        assertEquals(appData.resolve(AppInfo.DATA_DIRECTORY_NAME), resolved);
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

    @Test
    void defaultPathIsNotRepositoryRelativeDataDirectory() {
        Path mac = ApplicationPaths.resolveDatabasePath(
                props("os.name", "Mac OS X"),
                env(),
                tempHome
        );
        assertFalse(mac.toString().contains("/data/" + ApplicationPaths.DATABASE_FILE_NAME));
        assertTrue(mac.toString().contains("Application Support"));
        assertTrue(mac.startsWith(tempHome.toAbsolutePath().normalize()));
    }

    @Test
    void linuxIgnoresRelativeXdgDataHome() {
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Linux"),
                env("XDG_DATA_HOME", "relative-data"),
                tempHome
        );
        assertEquals(tempHome.resolve(".local").resolve("share").resolve(AppInfo.DATA_DIRECTORY_NAME), resolved);
    }

    @Test
    void windowsIgnoresRelativeAppData() {
        Path resolved = ApplicationPaths.applicationDataDirectory(
                props("os.name", "Windows 11"),
                env("APPDATA", "relative-app-data"),
                tempHome
        );
        assertEquals(
                tempHome.resolve("AppData").resolve("Roaming").resolve(AppInfo.DATA_DIRECTORY_NAME),
                resolved
        );
    }

    @Test
    void relativeXdgDataHomeNeverResolvesUnderTheWorkingDirectory() {
        Path resolved = ApplicationPaths.resolveDatabasePath(
                props("os.name", "Linux"),
                env("XDG_DATA_HOME", "relative-data"),
                tempHome
        );
        assertTrue(resolved.startsWith(tempHome.toAbsolutePath().normalize()), resolved.toString());
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
