package com.manpreet.bank.ui;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test double that keeps the theme preference in memory only.
 */
final class InMemoryThemePreferenceStore implements ThemePreferenceStore {

    private final AtomicReference<String> stored = new AtomicReference<>();

    InMemoryThemePreferenceStore() {
    }

    InMemoryThemePreferenceStore(String initialValue) {
        stored.set(initialValue);
    }

    @Override
    public Optional<String> load() {
        return Optional.ofNullable(stored.get());
    }

    @Override
    public void save(Theme theme) {
        Objects.requireNonNull(theme, "theme must not be null");
        stored.set(theme.name());
    }

    Optional<String> peek() {
        return Optional.ofNullable(stored.get());
    }
}
