package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards against attribute values that FXMLLoader treats as variable references.
 * A value starting with {@code $} resolves an fx:id (for example {@code $0.00} fails with "Invalid path"
 * and the screen does not load). Literal dollar signs must be escaped as {@code \$}.
 */
class FxmlVariableReferenceTest {

    private static final Pattern FX_ID = Pattern.compile("fx:id=\"([^\"]+)\"");
    private static final Pattern ATTRIBUTE_VALUE = Pattern.compile("\\s[\\w:.]+=\"([^\"]*)\"");

    @Test
    void fxmlDollarValuesReferenceDefinedIds() throws IOException {
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
                for (String value : findUnresolvedReferences(Files.readString(fxmlFile))) {
                    violations.add(fxmlFile.getFileName() + ": \"" + value + "\"");
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("FXML values starting with $ must reference an fx:id; escape literal dollar signs as \\$:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    void detectorFlagsUnescapedLiteralsAndAllowsReferencesAndEscapes() {
        assertFalse(findUnresolvedReferences("<Label text=\"$0.00\"/>").isEmpty());
        assertFalse(findUnresolvedReferences("<Label text=\"$25\"/>").isEmpty());
        assertFalse(findUnresolvedReferences("<Label labelFor=\"$missingField\"/>").isEmpty());

        assertTrue(findUnresolvedReferences("<Label text=\"\\$0.00\"/>").isEmpty());
        assertTrue(findUnresolvedReferences(
                "<TextField fx:id=\"amountField\"/><Label labelFor=\"$amountField\"/>").isEmpty());
        assertTrue(findUnresolvedReferences(
                "<TextField fx:id=\"amountField\"/><Label text=\"$amountField.text\"/>").isEmpty());
        assertTrue(findUnresolvedReferences("<Label text=\"${amountField.text}\"/>").isEmpty());
        assertTrue(findUnresolvedReferences("<Label text=\"Total $0.00\"/>").isEmpty());
    }

    static List<String> findUnresolvedReferences(String content) {
        Set<String> ids = new HashSet<>();
        Matcher idMatcher = FX_ID.matcher(content);
        while (idMatcher.find()) {
            ids.add(idMatcher.group(1));
        }

        List<String> unresolved = new ArrayList<>();
        Matcher valueMatcher = ATTRIBUTE_VALUE.matcher(content);
        while (valueMatcher.find()) {
            String value = valueMatcher.group(1);
            // "${...}" is a binding expression; "\$" is an escaped literal.
            if (!value.startsWith("$") || value.startsWith("${")) {
                continue;
            }
            String reference = value.substring(1);
            int dot = reference.indexOf('.');
            String root = dot >= 0 ? reference.substring(0, dot) : reference;
            if (!ids.contains(root)) {
                unresolved.add(value);
            }
        }
        return unresolved;
    }
}
