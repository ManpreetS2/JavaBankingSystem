package com.manpreet.bank.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.scene.Scene;

/**
 * Applies light/dark stylesheets to application scenes.
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

    private void applyTo(Scene scene) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(resource(BASE));
        scene.getStylesheets().add(resource(COMPONENTS));
        scene.getStylesheets().add(resource(currentTheme == Theme.DARK ? DARK : LIGHT));
    }

    private static String resource(String path) {
        return Objects.requireNonNull(
                ThemeManager.class.getResource(path),
                "Missing stylesheet: " + path
        ).toExternalForm();
    }
}
