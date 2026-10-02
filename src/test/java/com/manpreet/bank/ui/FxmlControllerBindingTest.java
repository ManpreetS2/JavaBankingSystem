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
 * Lightweight source check that FXML fx:id / onAction names exist in the declared controller.
 */
class FxmlControllerBindingTest {

    private static final Pattern CONTROLLER = Pattern.compile("fx:controller=\"([^\"]+)\"");
    private static final Pattern FX_ID = Pattern.compile("fx:id=\"([^\"]+)\"");
    private static final Pattern ON_ACTION = Pattern.compile("onAction=\"#([^\"]+)\"");

    @Test
    void fxmlIdsAndActionsExistOnControllers() throws IOException {
        Path fxmlDir = Path.of("src/main/resources/fxml");
        Path controllerDir = Path.of("src/main/java/com/manpreet/bank/controller");
        assertTrue(Files.isDirectory(fxmlDir));
        assertTrue(Files.isDirectory(controllerDir));

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.list(fxmlDir)) {
            List<Path> files = paths.filter(p -> p.toString().endsWith(".fxml")).sorted().toList();
            assertFalse(files.isEmpty());
            for (Path fxml : files) {
                String content = Files.readString(fxml);
                Matcher controllerMatch = CONTROLLER.matcher(content);
                if (!controllerMatch.find()) {
                    violations.add(fxml + ": missing fx:controller");
                    continue;
                }
                String controllerFqn = controllerMatch.group(1);
                String simpleName = controllerFqn.substring(controllerFqn.lastIndexOf('.') + 1);
                Path javaFile = controllerDir.resolve(simpleName + ".java");
                if (!Files.exists(javaFile)) {
                    violations.add(fxml + ": missing controller file " + javaFile);
                    continue;
                }
                String java = Files.readString(javaFile);
                Matcher idMatcher = FX_ID.matcher(content);
                while (idMatcher.find()) {
                    String id = idMatcher.group(1);
                    if (!java.contains(id)) {
                        violations.add(fxml.getFileName() + ": fx:id \"" + id + "\" not found in " + simpleName);
                    }
                }
                Matcher actionMatcher = ON_ACTION.matcher(content);
                while (actionMatcher.find()) {
                    String action = actionMatcher.group(1);
                    if (!java.contains("void " + action + "(") && !java.contains("void " + action + " (")) {
                        violations.add(fxml.getFileName() + ": onAction \"#" + action
                                + "\" missing method in " + simpleName);
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("FXML binding problems:\n" + String.join("\n", violations));
        }
    }
}
