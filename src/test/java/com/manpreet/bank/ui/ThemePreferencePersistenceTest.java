package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ThemePreferencePersistenceTest {

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
    void loadRuntimeFailureDefaultsToLight() {
        ThemePreferenceStore failingStore = new ThemePreferenceStore() {
            @Override
            public Optional<String> load() {
                throw new RuntimeException("preferences unavailable");
            }

            @Override
            public void save(Theme theme) {
                throw new UnsupportedOperationException("not used");
            }
        };

        ThemeManager manager = new ThemeManager(failingStore);
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
        assertTrue(manager.stylesheetPaths().get(2).endsWith("/css/theme-light.css"));
    }

    @Test
    void loadSecurityExceptionDefaultsToLight() {
        ThemePreferenceStore failingStore = new ThemePreferenceStore() {
            @Override
            public Optional<String> load() {
                throw new SecurityException("preferences denied");
            }

            @Override
            public void save(Theme theme) {
                throw new UnsupportedOperationException("not used");
            }
        };

        ThemeManager manager = new ThemeManager(failingStore);
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
    }

    @Test
    void setThemeLeavesCurrentThemeUnchangedWhenPersistenceFails() {
        ThemePreferenceStore failingStore = new ThemePreferenceStore() {
            @Override
            public Optional<String> load() {
                return Optional.empty();
            }

            @Override
            public void save(Theme theme) {
                throw new IllegalStateException("Unable to save theme preference");
            }
        };
        ThemeManager manager = new ThemeManager(failingStore);
        AtomicReference<List<String>> applied = new AtomicReference<>(List.of());
        manager.registerManagedTarget(applied::set);

        assertThrows(IllegalStateException.class, () -> manager.setTheme(Theme.DARK));
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
        assertTrue(endsWithTheme(applied.get(), "/css/theme-light.css"));
    }

    @Test
    void registeredTargetsUpdateWhenThemeChanges() {
        InMemoryThemePreferenceStore store = new InMemoryThemePreferenceStore();
        ThemeManager manager = new ThemeManager(store);
        AtomicReference<List<String>> applied = new AtomicReference<>(List.of());
        List<List<String>> history = new ArrayList<>();

        manager.registerManagedTarget(urls -> {
            List<String> copy = List.copyOf(urls);
            applied.set(copy);
            history.add(copy);
        });

        assertEquals(1, history.size());
        assertTrue(endsWithTheme(applied.get(), "/css/theme-light.css"));

        manager.setTheme(Theme.DARK);
        assertTrue(endsWithTheme(applied.get(), "/css/theme-dark.css"));
        assertEquals(Optional.of("DARK"), store.peek());

        manager.setTheme(Theme.LIGHT);
        assertTrue(endsWithTheme(applied.get(), "/css/theme-light.css"));
        assertEquals(Optional.of("LIGHT"), store.peek());
        assertEquals(3, history.size());
    }

    @Test
    void settingsExplainsThemeIsRememberedAndBindingsStayValid() throws Exception {
        String fxml = Files.readString(Path.of("src/main/resources/fxml/settings.fxml"));
        assertFalse(fxml.contains("until you close the application"));
        assertTrue(fxml.contains("remembered the next time you open the application"));
        assertTrue(fxml.contains("fx:controller=\"com.manpreet.bank.controller.SettingsController\""));
        assertTrue(fxml.contains("fx:id=\"lightThemeOption\""));
        assertTrue(fxml.contains("fx:id=\"darkThemeOption\""));

        String controller = Files.readString(
                Path.of("src/main/java/com/manpreet/bank/controller/SettingsController.java"));
        assertTrue(controller.contains("lightThemeOption"));
        assertTrue(controller.contains("darkThemeOption"));
        assertTrue(controller.contains("themeManager().setTheme(theme)"));
        assertTrue(controller.contains("catch (RuntimeException e)"));
        assertTrue(controller.contains("selectCurrentTheme()"));
    }

    private static boolean endsWithTheme(List<String> urls, String suffix) {
        return urls.stream().anyMatch(url -> url.endsWith(suffix));
    }
}
