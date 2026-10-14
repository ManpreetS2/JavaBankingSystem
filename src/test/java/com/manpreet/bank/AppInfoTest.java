package com.manpreet.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppInfoTest {

    @Test
    void packageVersionIsNumericForJpackage() {
        assertTrue(AppInfo.PACKAGE_VERSION.matches("\\d+(\\.\\d+)*"));
        assertFalse(AppInfo.PACKAGE_VERSION.toUpperCase().contains("SNAPSHOT"));
    }

    @Test
    void displayVersionMatchesRelease() {
        assertEquals("1.0.0", AppInfo.VERSION);
        assertEquals("1.0.0", AppInfo.PACKAGE_VERSION);
        assertFalse(AppInfo.VERSION.toUpperCase().contains("SNAPSHOT"));
        assertTrue(AppInfo.displayNameWithVersion().contains(AppInfo.VERSION));
    }

    @Test
    void windowTitlesUseProductName() {
        assertEquals(AppInfo.APPLICATION_NAME, AppInfo.windowTitle());
        assertEquals(AppInfo.APPLICATION_NAME + " — Sign in", AppInfo.windowTitle("Sign in"));
        assertEquals(AppInfo.APPLICATION_NAME, AppInfo.windowTitle("  "));
    }
}
