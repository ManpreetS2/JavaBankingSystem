package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.manpreet.bank.exception.InsufficientFundsException;
import com.manpreet.bank.exception.ValidationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UiSupportTest {

    @Test
    void themeManagerTracksCurrentThemeAndResolvesStylesheets() {
        InMemoryThemePreferenceStore store = new InMemoryThemePreferenceStore();
        ThemeManager manager = new ThemeManager(store);
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
        List<String> lightPaths = manager.stylesheetPaths();
        assertEquals(3, lightPaths.size());
        assertTrue(lightPaths.get(0).endsWith("/css/base.css"));
        assertTrue(lightPaths.get(1).endsWith("/css/components.css"));
        assertTrue(lightPaths.get(2).endsWith("/css/theme-light.css"));
        for (String path : lightPaths) {
            assertNotNull(ThemeManager.class.getResource(path), "Missing stylesheet resource: " + path);
        }

        manager.toggleTheme();
        assertEquals(Theme.DARK, manager.getCurrentTheme());
        assertEquals(Optional.of("DARK"), store.peek());
        assertTrue(manager.stylesheetPaths().get(2).endsWith("/css/theme-dark.css"));
        assertNotNull(ThemeManager.class.getResource(manager.stylesheetPaths().get(2)));

        manager.setTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
        assertEquals(Optional.of("LIGHT"), store.peek());
    }

    @Test
    void uiWindowsReturnsNullForMissingNode() {
        assertNull(UiWindows.from(null));
    }

    @Test
    void uiErrorMapperUsesSafeDomainMessages() {
        assertEquals("Amount must be greater than zero",
                UiErrorMapper.toUserMessage(new ValidationException("Amount must be greater than zero")));
        assertEquals("Insufficient funds for this withdrawal",
                UiErrorMapper.toUserMessage(new InsufficientFundsException("Insufficient funds for this withdrawal")));
        assertEquals("Something went wrong. Please try again.",
                UiErrorMapper.toUserMessage(new RuntimeException("select * from users")));
    }

    @Test
    void dashboardViewModelHoldsReferences() {
        DashboardViewModel model = new DashboardViewModel(null, null, null, null, java.util.List.of());
        assertSame(java.util.List.of(), model.recentTransactions());
    }
}
