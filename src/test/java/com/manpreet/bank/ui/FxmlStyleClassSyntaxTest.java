package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards against space-separated multi-class {@code styleClass} attributes in FXML.
 * JavaFX requires comma-separated values for collection attributes.
 */
class FxmlStyleClassSyntaxTest {

    private static final Pattern STYLE_CLASS_ATTRIBUTE =
            Pattern.compile("styleClass\\s*=\\s*\"([^\"]*)\"");

    @Test
    void fxmlStyleClassAttributesUseCommaSeparatedLists() throws IOException {
        Path fxmlDir = Path.of("src/main/resources/fxml");
        assertTrue(Files.isDirectory(fxmlDir), "Expected FXML directory at " + fxmlDir.toAbsolutePath());

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.list(fxmlDir)) {
            List<Path> fxmlFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".fxml"))
                    .sorted()
                    .toList();
            assertFalse(fxmlFiles.isEmpty(), "Expected at least one FXML file");
            for (Path fxmlFile : fxmlFiles) {
                violations.addAll(findViolations(fxmlFile));
            }
        }

        if (!violations.isEmpty()) {
            fail("Invalid multi-class styleClass syntax (use commas):\n" + String.join("\n", violations));
        }
    }

    @Test
    void detectorFlagsSpaceSeparatedClassesAndAllowsCommaLists() {
        assertTrue(containsInvalidStyleClass("styleClass=\"page-subtitle body-secondary\""));
        assertTrue(containsInvalidStyleClass("styleClass=\"button-primary button-compact\""));
        assertFalse(containsInvalidStyleClass("styleClass=\"page-subtitle, body-secondary\""));
        assertFalse(containsInvalidStyleClass("styleClass=\"button-primary\""));
        assertFalse(containsInvalidStyleClass("styleClass=\"button-primary,button-compact\""));
        assertFalse(containsInvalidStyleClass("text=\"classA classB\""));
    }

    static boolean containsInvalidStyleClass(String content) {
        return !findViolationsInContent("inline", content).isEmpty();
    }

    private static List<String> findViolations(Path fxmlFile) throws IOException {
        return findViolationsInContent(fxmlFile.toString(), Files.readString(fxmlFile));
    }

    private static List<String> findViolationsInContent(String source, String content) {
        List<String> violations = new ArrayList<>();
        Matcher matcher = STYLE_CLASS_ATTRIBUTE.matcher(content);
        while (matcher.find()) {
            String value = matcher.group(1);
            String[] parts = value.split(",", -1);
            for (String part : parts) {
                String token = part.trim();
                if (token.isEmpty()) {
                    continue;
                }
                if (token.chars().anyMatch(Character::isWhitespace)) {
                    violations.add(source + ": styleClass=\"" + value + "\"");
                    break;
                }
            }
        }
        return violations;
    }
}
