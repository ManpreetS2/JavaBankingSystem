package com.manpreet.bank.ui;

import java.util.Objects;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Stores the application theme in {@link Preferences} under the UI package node.
 */
public final class PreferencesThemePreferenceStore implements ThemePreferenceStore {

    static final String KEY = "theme";

    private final Preferences preferences;

    public PreferencesThemePreferenceStore() {
        this(Preferences.userNodeForPackage(ThemeManager.class));
    }

    PreferencesThemePreferenceStore(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences must not be null");
    }

    @Override
    public Optional<String> load() {
        String value = preferences.get(KEY, null);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @Override
    public void save(Theme theme) {
        Objects.requireNonNull(theme, "theme must not be null");
        preferences.put(KEY, theme.name());
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Unable to save theme preference", e);
        }
    }
}
