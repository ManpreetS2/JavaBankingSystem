package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ThemePreferencePersistenceTest {

    @BeforeAll
    static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    void missingPreferenceDefaultsToLight() {
        ThemeManager manager = new ThemeManager(new InMemoryThemePreferenceStore());
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
        assertTrue(manager.stylesheetPaths().get(2).endsWith("/css/theme-light.css"));
    }

    @Test
    void storedLightLoadsAsLight() {
        ThemeManager manager = new ThemeManager(new InMemoryThemePreferenceStore("LIGHT"));
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
    }

    @Test
    void storedDarkLoadsAsDark() {
        ThemeManager manager = new ThemeManager(new InMemoryThemePreferenceStore("DARK"));
        assertEquals(Theme.DARK, manager.getCurrentTheme());
        assertTrue(manager.stylesheetPaths().get(2).endsWith("/css/theme-dark.css"));
    }

    @Test
    void setThemeDarkPersistsDark() {
        InMemoryThemePreferenceStore store = new InMemoryThemePreferenceStore();
        ThemeManager manager = new ThemeManager(store);

        manager.setTheme(Theme.DARK);

        assertEquals(Theme.DARK, manager.getCurrentTheme());
        assertEquals(Optional.of("DARK"), store.peek());
    }

    @Test
    void setThemeLightPersistsLight() {
        InMemoryThemePreferenceStore store = new InMemoryThemePreferenceStore("DARK");
        ThemeManager manager = new ThemeManager(store);

        manager.setTheme(Theme.LIGHT);

        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
        assertEquals(Optional.of("LIGHT"), store.peek());
    }

    @Test
    void invalidStoredValueFallsBackToLight() {
        assertEquals(Theme.LIGHT, ThemeManager.resolveTheme(Optional.of("neon")));
        assertEquals(Theme.LIGHT, ThemeManager.resolveTheme(Optional.of("   ")));
        assertEquals(Theme.LIGHT, ThemeManager.resolveTheme(Optional.empty()));

        ThemeManager manager = new ThemeManager(new InMemoryThemePreferenceStore("not-a-theme"));
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
    }

    @Test
    void registeredScenesUpdateWhenThemeChanges() throws Exception {
        InMemoryThemePreferenceStore store = new InMemoryThemePreferenceStore();
        ThemeManager manager = new ThemeManager(store);
        Scene scene = createSceneOnFxThread();

        manager.registerScene(scene);
        assertTrue(stylesheetEndsWith(scene, "/css/theme-light.css"));

        manager.setTheme(Theme.DARK);
        assertTrue(stylesheetEndsWith(scene, "/css/theme-dark.css"));
        assertEquals(Optional.of("DARK"), store.peek());

        manager.setTheme(Theme.LIGHT);
        assertTrue(stylesheetEndsWith(scene, "/css/theme-light.css"));
        assertEquals(Optional.of("LIGHT"), store.peek());
    }

    @Test
    void settingsExplainsThemeIsRemembered() throws Exception {
        String fxml = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/fxml/settings.fxml"));
        assertFalse(fxml.contains("until you close the application"));
        assertTrue(fxml.contains("remembered the next time you open the application"));
    }

    private static Scene createSceneOnFxThread() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Scene[] holder = new Scene[1];
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                holder[0] = new Scene(new Region(), 100, 100);
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out creating Scene");
        if (error[0] != null) {
            if (error[0] instanceof Error e) {
                throw e;
            }
            if (error[0] instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(error[0]);
        }
        return holder[0];
    }

    private static boolean stylesheetEndsWith(Scene scene, String suffix) {
        return scene.getStylesheets().stream().anyMatch(url -> url.endsWith(suffix));
    }
}
