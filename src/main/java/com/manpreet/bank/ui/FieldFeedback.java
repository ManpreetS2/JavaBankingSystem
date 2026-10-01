package com.manpreet.bank.ui;

import java.util.Objects;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;

/**
 * Marks individual form fields as invalid using the shared {@code input-error} style.
 * The error message is also exposed as accessible help so it is not conveyed by color alone.
 */
public final class FieldFeedback {

    private static final String ERROR_STYLE_CLASS = "input-error";
    private static final String ORIGINAL_HELP_KEY = FieldFeedback.class.getName() + ".originalHelp";

    private FieldFeedback() {
    }

    public static void markInvalid(Control field, String message) {
        Objects.requireNonNull(field, "field must not be null");
        if (!field.getStyleClass().contains(ERROR_STYLE_CLASS)) {
            field.getStyleClass().add(ERROR_STYLE_CLASS);
        }
        // containsKey rather than putIfAbsent: the original help may legitimately be null.
        if (!field.getProperties().containsKey(ORIGINAL_HELP_KEY)) {
            field.getProperties().put(ORIGINAL_HELP_KEY, field.getAccessibleHelp());
        }
        field.setAccessibleHelp(message);
    }

    /**
     * Removes the invalid marker and restores any accessible help the field had before it was marked.
     */
    public static void clear(Control field) {
        Objects.requireNonNull(field, "field must not be null");
        field.getStyleClass().remove(ERROR_STYLE_CLASS);
        if (field.getProperties().containsKey(ORIGINAL_HELP_KEY)) {
            field.setAccessibleHelp((String) field.getProperties().remove(ORIGINAL_HELP_KEY));
        }
    }

    /**
     * Removes the invalid marker as soon as the user edits the field.
     */
    public static void clearWhenEdited(TextInputControl field) {
        Objects.requireNonNull(field, "field must not be null");
        field.textProperty().addListener((observable, previous, current) -> clear(field));
    }
}
