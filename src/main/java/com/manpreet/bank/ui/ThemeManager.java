package com.manpreet.bank.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Applies light/dark stylesheets to application scenes and transient dialog roots.
 */
public class ThemeManager {

    private static final String BASE = "/css/base.css";
    private static final String COMPONENTS = "/css/components.css";
    private static final String LIGHT = "/css/theme-light.css";
    private static final String DARK = "/css/theme-dark.css";

    private final List<Scene> managedScenes = new ArrayList<>();
    private Theme currentTheme = Theme.LIGHT;

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public synchronized void registerScene(Scene scene) {
        Objects.requireNonNull(scene, "scene must not be null");
        if (!managedScenes.contains(scene)) {
            managedScenes.add(scene);
        }
        applyTo(scene);
    }

    public synchronized void setTheme(Theme theme) {
        this.currentTheme = Objects.requireNonNull(theme, "theme must not be null");
        for (Scene scene : List.copyOf(managedScenes)) {
            applyTo(scene);
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

    private static String resource(String path) {
        return Objects.requireNonNull(
                ThemeManager.class.getResource(path),
                "Missing stylesheet: " + path
        ).toExternalForm();
    }
}
