package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void combinationsMatchRuntimeSceneAccelerators() {
        assertCombination(ShellKeyboardShortcuts.Action.DASHBOARD, KeyCode.DIGIT1);
        assertCombination(ShellKeyboardShortcuts.Action.ACCOUNTS, KeyCode.DIGIT2);
        assertCombination(ShellKeyboardShortcuts.Action.TRANSACTIONS, KeyCode.DIGIT3);
        assertCombination(ShellKeyboardShortcuts.Action.SETTINGS, KeyCode.DIGIT4);
        assertCombination(ShellKeyboardShortcuts.Action.FOCUS_SEARCH, KeyCode.F);

        KeyCodeCombination settingsComma = ShellKeyboardShortcuts.settingsComma();
        assertEquals(KeyCode.COMMA, settingsComma.getCode());
        assertEquals(KeyCombination.ModifierValue.DOWN, settingsComma.getShortcut());
    }

    @Test
    void focusSearchIsContextualFindNotNavigation() {
        assertTrue(ShellKeyboardShortcuts.allowsFocusSearch(true, true));
        assertFalse(ShellKeyboardShortcuts.allowsFocusSearch(false, true));
        assertFalse(ShellKeyboardShortcuts.allowsFocusSearch(true, false));
        assertFalse(ShellKeyboardShortcuts.allowsFocusSearch(false, false));
    }

    @Test
    void shellWiresAcceleratorsAndKeepsFocusSearchContextual() throws IOException {
        String shell = Files.readString(Path.of(
                "src/main/java/com/manpreet/bank/controller/MainShellController.java"));
        assertTrue(shell.contains("ShellKeyboardShortcuts.combination(action)"));
        assertTrue(shell.contains("ShellKeyboardShortcuts.settingsComma()"));
        assertTrue(shell.contains("clearKeyboardShortcuts()"));
        assertTrue(shell.contains("allowsFocusSearch("));

        int methodStart = shell.indexOf("private void focusTransactionsSearch()");
        assertTrue(methodStart >= 0, "focusTransactionsSearch must exist");
        int methodEnd = shell.indexOf("\n    private void ", methodStart + 1);
        assertTrue(methodEnd > methodStart);
        String method = shell.substring(methodStart, methodEnd);
        assertFalse(method.contains("showTransactions()"),
                "Shortcut+F must not navigate to Transactions");
        assertTrue(method.contains("return;"));
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

    private static void assertCombination(ShellKeyboardShortcuts.Action action, KeyCode code) {
        KeyCodeCombination combination = ShellKeyboardShortcuts.combination(action);
        assertEquals(code, combination.getCode(), "Unexpected key for " + action);
        assertEquals(KeyCombination.ModifierValue.DOWN, combination.getShortcut(),
                "Expected Shortcut modifier for " + action);
    }
}
