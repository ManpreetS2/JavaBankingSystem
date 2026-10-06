package com.manpreet.bank.ui;

import java.util.Objects;
import java.util.Optional;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * Platform-neutral shell keyboard shortcuts using the JavaFX Shortcut modifier.
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

    public static Optional<Action> resolve(KeyEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return resolve(event.getCode(), event.isShortcutDown(), event.isAltDown());
    }

    /**
     * Resolves a pressed key to a shell action when Shortcut is held.
     * Digit and numpad equivalents map to the same destinations.
     */
    static Optional<Action> resolve(KeyCode code, boolean shortcutDown, boolean altDown) {
        Objects.requireNonNull(code, "code must not be null");
        if (!shortcutDown || altDown) {
            return Optional.empty();
        }
        return switch (code) {
            case DIGIT1, NUMPAD1 -> Optional.of(Action.DASHBOARD);
            case DIGIT2, NUMPAD2 -> Optional.of(Action.ACCOUNTS);
            case DIGIT3, NUMPAD3 -> Optional.of(Action.TRANSACTIONS);
            case DIGIT4, NUMPAD4 -> Optional.of(Action.SETTINGS);
            case F -> Optional.of(Action.FOCUS_SEARCH);
            case COMMA -> Optional.of(Action.SETTINGS);
            default -> Optional.empty();
        };
    }
}
