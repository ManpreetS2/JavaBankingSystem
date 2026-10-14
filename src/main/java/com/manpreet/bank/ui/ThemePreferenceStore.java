package com.manpreet.bank.ui;

import java.util.Optional;

/**
 * Application-level storage for the selected visual theme.
 * This is a local preference, not banking data.
 */
public interface ThemePreferenceStore {

    /**
     * Returns the raw stored theme value when present.
     * Callers must validate the value; corrupt data is allowed here.
     */
    Optional<String> load();

    /**
     * Persists the given theme for later restarts.
     */
    void save(Theme theme);
}
