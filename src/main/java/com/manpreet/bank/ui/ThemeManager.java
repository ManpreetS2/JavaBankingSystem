package com.manpreet.bank.ui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Applies light/dark stylesheets to application scenes and transient dialog roots.
 * Loads and persists the selected theme through {@link ThemePreferenceStore}.
 */
public class ThemeManager {

    private static final String BASE = "/css/base.css";
    private static final String COMPONENTS = "/css/components.css";
    private static final String LIGHT = "/css/theme-light.css";
    private static final String DARK = "/css/theme-dark.css";

    private final ThemePreferenceStore preferenceStore;
    private final IdentityHashMap<Scene, Boolean> managedScenes = new IdentityHashMap<>();
    private final List<Consumer<List<String>>> managedTargets = new ArrayList<>();
    private Theme currentTheme;

    public ThemeManager() {
        this(new PreferencesThemePreferenceStore());
    }

    public ThemeManager(ThemePreferenceStore preferenceStore) {
        this.preferenceStore = Objects.requireNonNull(preferenceStore, "preferenceStore must not be null");
        this.currentTheme = loadInitialTheme(preferenceStore);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public synchronized void registerScene(Scene scene) {
        Objects.requireNonNull(scene, "scene must not be null");
        if (managedScenes.put(scene, Boolean.TRUE) == null) {
            managedTargets.add(urls -> scene.getStylesheets().setAll(urls));
        }
        applyTo(scene);
    }

    /**
     * Registers a stylesheet consumer that is updated whenever the theme changes.
     * Package-private so tests can verify managed updates without creating JavaFX scenes.
     */
    synchronized void registerManagedTarget(Consumer<List<String>> target) {
        Objects.requireNonNull(target, "target must not be null");
        managedTargets.add(target);
        target.accept(currentStylesheetUrls());
    }

    public synchronized void setTheme(Theme theme) {
        Theme next = Objects.requireNonNull(theme, "theme must not be null");
        // Persist first so a successful setTheme always means the preference was written.
        preferenceStore.save(next);
        this.currentTheme = next;
        List<String> urls = currentStylesheetUrls();
        for (Consumer<List<String>> target : List.copyOf(managedTargets)) {
            target.accept(urls);
        }
    }

    public synchronized void toggleTheme() {
        setTheme(currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }

    /**
     * Applies the current application stylesheets to a scene.
     * Managed scenes are also updated when the theme changes.
     */
    public synchronized void applyTo(Scene scene) {
        Objects.requireNonNull(scene, "scene must not be null");
        scene.getStylesheets().setAll(currentStylesheetUrls());
    }

    /**
     * Applies the current application stylesheets to a parent root such as a {@code DialogPane}.
     * Does not register the parent for future theme updates (suitable for short-lived dialogs).
     */
    public synchronized void applyTo(Parent parent) {
        Objects.requireNonNull(parent, "parent must not be null");
        parent.getStylesheets().setAll(currentStylesheetUrls());
    }

    private List<String> currentStylesheetUrls() {
        return stylesheetPaths().stream().map(ThemeManager::resource).toList();
    }

    /**
     * Classpath stylesheet paths for the current theme (base, components, theme).
     * Useful for tests that verify resources resolve without creating JavaFX nodes.
     */
    List<String> stylesheetPaths() {
        return List.of(
                BASE,
                COMPONENTS,
                currentTheme == Theme.DARK ? DARK : LIGHT
        );
    }

    /**
     * Resolves the startup theme from preferences.
     * Missing, invalid, or unloadable preferences degrade to {@link Theme#LIGHT}
     * so a cosmetic preference never blocks banking application startup.
     */
    private static Theme loadInitialTheme(ThemePreferenceStore preferenceStore) {
        try {
            return resolveTheme(preferenceStore.load());
        } catch (RuntimeException ignored) {
            return Theme.LIGHT;
        }
    }

    static Theme resolveTheme(Optional<String> stored) {
        if (stored.isEmpty()) {
            return Theme.LIGHT;
        }
        String raw = stored.get().trim();
        if (raw.isEmpty()) {
            return Theme.LIGHT;
        }
        try {
            return Theme.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Theme.LIGHT;
        }
    }

    private static String resource(String path) {
        return Objects.requireNonNull(
                ThemeManager.class.getResource(path),
                "Missing stylesheet: " + path
        ).toExternalForm();
    }
}
