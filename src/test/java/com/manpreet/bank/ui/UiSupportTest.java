package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.manpreet.bank.exception.InsufficientFundsException;
import com.manpreet.bank.exception.ValidationException;
import org.junit.jupiter.api.Test;

class UiSupportTest {

    @Test
    void themeManagerTracksCurrentTheme() {
        ThemeManager manager = new ThemeManager();
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
        manager.toggleTheme();
        assertEquals(Theme.DARK, manager.getCurrentTheme());
        manager.setTheme(Theme.LIGHT);
        assertEquals(Theme.LIGHT, manager.getCurrentTheme());
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
