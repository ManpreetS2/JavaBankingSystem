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
        String previous = null;
        boolean hadPrevious = false;
        try {
            previous = preferences.get(KEY, null);
            hadPrevious = previous != null;
            preferences.put(KEY, theme.name());
            preferences.flush();
        } catch (BackingStoreException | SecurityException e) {
            restorePrevious(hadPrevious, previous);
            throw new IllegalStateException("Unable to save theme preference");
        }
    }

    private void restorePrevious(boolean hadPrevious, String previous) {
        try {
            if (hadPrevious) {
                preferences.put(KEY, previous);
            } else {
                preferences.remove(KEY);
            }
            preferences.flush();
        } catch (BackingStoreException | SecurityException ignored) {
            // Best-effort restoration after a failed persistence attempt.
        }
    }
}
