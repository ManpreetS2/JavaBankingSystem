package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The shared button and sidebar styles replace the default focus ring, so both themes must define a
 * keyboard focus state for each of them. Without it, focus was not visible on any button in the app.
 */
class FocusStyleCoverageTest {

    private static final List<String> FOCUSABLE_STYLE_CLASSES = List.of(
            ".button-primary",
            ".button-secondary",
            ".button-ghost",
            ".button-danger",
            ".sidebar-item",
            ".sidebar-item-active",
            ".combo-box",
            ".date-picker",
            ".radio-button"
    );

    @Test
    void bothThemesDefineAKeyboardFocusStateForEveryCustomButtonStyle() throws IOException {
        for (String theme : List.of("theme-light.css", "theme-dark.css")) {
            String css = Files.readString(Path.of("src/main/resources/css", theme));
            for (String styleClass : FOCUSABLE_STYLE_CLASSES) {
                assertTrue(css.contains(styleClass + ":focus-visible"),
                        theme + " has no " + styleClass + ":focus-visible rule");
            }
        }
    }

    @Test
    void focusRingsDoNotUseLayoutAffectingBorders() throws IOException {
        // Borders add to a region's insets and would resize the button when it gains focus.
        for (String theme : List.of("theme-light.css", "theme-dark.css")) {
            String css = Files.readString(Path.of("src/main/resources/css", theme));
            int index = css.indexOf(":focus-visible");
            while (index >= 0) {
                int open = css.indexOf('{', index);
                int close = css.indexOf('}', open);
                String body = css.substring(open, close);
                assertTrue(!body.contains("-fx-border-width") && !body.contains("-fx-padding"),
                        theme + " focus rule changes layout: " + body.trim());
                index = css.indexOf(":focus-visible", close);
            }
        }
    }
}
