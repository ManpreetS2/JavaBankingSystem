package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import org.junit.jupiter.api.Test;

class PreferencesThemePreferenceStoreTest {

    @Test
    void successfulSavePersistsThemeName() {
        ControllablePreferences preferences = new ControllablePreferences();
        PreferencesThemePreferenceStore store = new PreferencesThemePreferenceStore(preferences);

        store.save(Theme.DARK);

        assertEquals(Optional.of("DARK"), store.load());
        assertEquals(1, preferences.flushCount);
    }

    @Test
    void failedFlushRestoresPreviousValue() {
        ControllablePreferences preferences = new ControllablePreferences();
        PreferencesThemePreferenceStore store = new PreferencesThemePreferenceStore(preferences);
        store.save(Theme.LIGHT);
        preferences.failNextFlush = true;

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> store.save(Theme.DARK));

        assertEquals("Unable to save theme preference", error.getMessage());
        assertEquals(Optional.of("LIGHT"), store.load());
    }

    @Test
    void failedFlushRemovesKeyWhenNoPreviousValueExisted() {
        ControllablePreferences preferences = new ControllablePreferences();
        PreferencesThemePreferenceStore store = new PreferencesThemePreferenceStore(preferences);
        preferences.failNextFlush = true;

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> store.save(Theme.DARK));

        assertEquals("Unable to save theme preference", error.getMessage());
        assertTrue(store.load().isEmpty());
        assertTrue(preferences.map.isEmpty());
    }

    /**
     * Isolated in-memory Preferences node — never touches the developer's real user prefs.
     */
    private static final class ControllablePreferences extends AbstractPreferences {

        private final Map<String, String> map = new HashMap<>();
        private boolean failNextFlush;
        private int flushCount;

        ControllablePreferences() {
            super(null, "");
        }

        @Override
        protected void putSpi(String key, String value) {
            map.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return map.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            map.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            map.clear();
        }

        @Override
        protected String[] keysSpi() {
            return map.keySet().toArray(String[]::new);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            throw new UnsupportedOperationException("test preferences have no children");
        }

        @Override
        protected void syncSpi() {
        }

        @Override
        protected void flushSpi() throws BackingStoreException {
            flushCount++;
            if (failNextFlush) {
                failNextFlush = false;
                throw new BackingStoreException("flush failed");
            }
        }
    }
}
