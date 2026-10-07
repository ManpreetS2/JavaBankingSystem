package com.manpreet.bank.ui;

import java.util.Objects;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Platform-neutral shell keyboard shortcuts using the JavaFX Shortcut modifier.
 *
 * <p>Runtime wiring uses {@link javafx.scene.Scene} accelerators with these
 * combinations. Shortcut+F is contextual Find: the shell focuses transaction
 * search only while Transactions is already active.
 */
public final class ShellKeyboardShortcuts {

    public enum Action {
        DASHBOARD,
        ACCOUNTS,
        TRANSACTIONS,
        SETTINGS,
        FOCUS_SEARCH
    }

    private ShellKeyboardShortcuts() {
    }

    public static KeyCodeCombination combination(Action action) {
        Objects.requireNonNull(action, "action must not be null");
        return switch (action) {
            case DASHBOARD -> new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN);
            case ACCOUNTS -> new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN);
            case TRANSACTIONS -> new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN);
            case SETTINGS -> new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.SHORTCUT_DOWN);
            case FOCUS_SEARCH -> new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
        };
    }

    /** Secondary Settings accelerator (Shortcut+,). */
    public static KeyCodeCombination settingsComma() {
        return new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN);
    }

    /**
     * Whether Shortcut+F should focus the Transactions search field.
     * Does not navigate; inactive sections leave Find as a no-op.
     */
    public static boolean allowsFocusSearch(boolean transactionsSectionActive, boolean searchFieldAvailable) {
        return transactionsSectionActive && searchFieldAvailable;
    }
}
