package com.manpreet.bank.ui;

import java.util.Objects;
import javafx.scene.control.Label;

/**
 * Applies consistent success / error / info messaging to status labels.
 */
public final class UiFeedback {

    public enum Kind {
        SUCCESS,
        ERROR,
        INFO
    }

    private UiFeedback() {
    }

    public static void show(Label label, Kind kind, String message) {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        label.getStyleClass().removeAll("success-text", "error-text", "info-text");
        label.getStyleClass().add(switch (kind) {
            case SUCCESS -> "success-text";
            case ERROR -> "error-text";
            case INFO -> "info-text";
        });
        label.setText(message == null ? "" : message);
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void success(Label label, String message) {
        show(label, Kind.SUCCESS, message);
    }

    public static void error(Label label, String message) {
        show(label, Kind.ERROR, message);
    }

    public static void info(Label label, String message) {
        show(label, Kind.INFO, message);
    }

    public static void clear(Label label) {
        Objects.requireNonNull(label, "label must not be null");
        label.setText("");
        label.getStyleClass().removeAll("success-text", "error-text", "info-text");
        label.getStyleClass().add("success-text");
    }
}
