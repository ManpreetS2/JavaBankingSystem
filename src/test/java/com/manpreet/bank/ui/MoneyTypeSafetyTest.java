package com.manpreet.bank.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards monetary service/repository code against accidental double/float calculations.
 */
class MoneyTypeSafetyTest {

    private static final Pattern DOUBLE_OR_FLOAT = Pattern.compile("\\b(double|float)\\b");

    private static final List<String> MONETARY_PATHS = List.of(
            "src/main/java/com/manpreet/bank/service",
            "src/main/java/com/manpreet/bank/repository",
            "src/main/java/com/manpreet/bank/util/MoneyUtil.java",
            "src/main/java/com/manpreet/bank/util/CurrencyFormatter.java",
            "src/main/java/com/manpreet/bank/model/Account.java",
            "src/main/java/com/manpreet/bank/model/Transaction.java"
    );

    @Test
    void monetaryPathsDoNotUsePrimitiveFloatingPoint() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String root : MONETARY_PATHS) {
            Path path = Path.of(root);
            if (Files.isRegularFile(path)) {
                scan(path, violations);
                continue;
            }
            try (Stream<Path> files = Files.walk(path)) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    try {
                        scan(p, violations);
                    } catch (IOException e) {
                        fail("Unable to read " + p + ": " + e.getMessage());
                    }
                });
            }
        }
        assertTrue(violations.isEmpty(), "Floating-point money usage found:\n" + String.join("\n", violations));
    }

    private static void scan(Path file, List<String> violations) throws IOException {
        List<String> lines = Files.readAllLines(file);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().startsWith("//") || line.trim().startsWith("*")) {
                continue;
            }
            if (DOUBLE_OR_FLOAT.matcher(line).find()) {
                violations.add(file + ":" + (i + 1) + ": " + line.trim());
            }
        }
    }
}
