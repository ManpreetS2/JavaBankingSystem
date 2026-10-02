package com.manpreet.bank.ui;

import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Small helpers for resolving application windows from UI nodes.
 */
public final class UiWindows {

    private UiWindows() {
    }

    public static Window from(Node node) {
        if (node == null || node.getScene() == null) {
            return null;
        }
        return node.getScene().getWindow();
    }
}
