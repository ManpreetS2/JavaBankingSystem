package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.Test;

class ShellKeyboardShortcutsTest {

    @Test
    void combinationsUsePlatformShortcutModifier() {
        for (ShellKeyboardShortcuts.Action action : ShellKeyboardShortcuts.Action.values()) {
            KeyCodeCombination combination = ShellKeyboardShortcuts.combination(action);
            assertEquals(KeyCombination.ModifierValue.DOWN, combination.getShortcut(),
                    "Expected Shortcut modifier for " + action);
        }
        assertEquals(KeyCode.DIGIT1, ShellKeyboardShortcuts.combination(ShellKeyboardShortcuts.Action.DASHBOARD).getCode());
        assertEquals(KeyCode.DIGIT2, ShellKeyboardShortcuts.combination(ShellKeyboardShortcuts.Action.ACCOUNTS).getCode());
        assertEquals(KeyCode.DIGIT3, ShellKeyboardShortcuts.combination(ShellKeyboardShortcuts.Action.TRANSACTIONS).getCode());
        assertEquals(KeyCode.DIGIT4, ShellKeyboardShortcuts.combination(ShellKeyboardShortcuts.Action.SETTINGS).getCode());
        assertEquals(KeyCode.F, ShellKeyboardShortcuts.combination(ShellKeyboardShortcuts.Action.FOCUS_SEARCH).getCode());
    }

    @Test
    void resolveMapsShortcutDigitsAndSearch() {
        assertEquals(
                ShellKeyboardShortcuts.Action.DASHBOARD,
                ShellKeyboardShortcuts.resolve(KeyCode.DIGIT1, true, false).orElseThrow());
        assertEquals(
                ShellKeyboardShortcuts.Action.ACCOUNTS,
                ShellKeyboardShortcuts.resolve(KeyCode.NUMPAD2, true, false).orElseThrow());
        assertEquals(
                ShellKeyboardShortcuts.Action.TRANSACTIONS,
                ShellKeyboardShortcuts.resolve(KeyCode.DIGIT3, true, false).orElseThrow());
        assertEquals(
                ShellKeyboardShortcuts.Action.SETTINGS,
                ShellKeyboardShortcuts.resolve(KeyCode.DIGIT4, true, false).orElseThrow());
        assertEquals(
                ShellKeyboardShortcuts.Action.FOCUS_SEARCH,
                ShellKeyboardShortcuts.resolve(KeyCode.F, true, false).orElseThrow());
        assertEquals(
                ShellKeyboardShortcuts.Action.SETTINGS,
                ShellKeyboardShortcuts.resolve(KeyCode.COMMA, true, false).orElseThrow());
        assertTrue(ShellKeyboardShortcuts.resolve(KeyCode.DIGIT1, false, false).isEmpty());
        assertTrue(ShellKeyboardShortcuts.resolve(KeyCode.DIGIT1, true, true).isEmpty());
    }

    @Test
    void transactionsSearchKeepsAccessiblePromptAndHelp() throws IOException {
        String transactions = Files.readString(Path.of("src/main/resources/fxml/transactions.fxml"));
        assertTrue(transactions.contains("promptText=\"Search transactions\""));
        assertTrue(transactions.contains("accessibleText=\"Search transactions\""));
        assertTrue(transactions.contains("accessibleHelp=\"Search transaction descriptions and activity\""));
        assertTrue(transactions.contains("No transactions match these filters"));

        String shell = Files.readString(Path.of("src/main/resources/fxml/main-shell.fxml"));
        assertTrue(shell.contains("sidebar-sign-out"));
        assertTrue(shell.contains("accessibleText=\"Sign out\""));
    }
}
