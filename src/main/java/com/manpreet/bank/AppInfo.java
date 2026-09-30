package com.manpreet.bank;

/**
 * Centralized product metadata for titles, diagnostics, and packaging.
 *
 * <p>{@link #VERSION} tracks the Maven {@code project.version} in {@code pom.xml}
 * ({@code 1.0-SNAPSHOT}). {@link #PACKAGE_VERSION} is the numeric jpackage
 * {@code app.packageVersion} ({@code 1.0.0}) because native packaging rejects SNAPSHOT
 * suffixes. Keep those two pom properties and these constants in sync when bumping releases.
 */
public final class AppInfo {

    public static final String APPLICATION_NAME = "Banking System";
    /** Maven project.version — development/display version. */
    public static final String VERSION = "1.0-SNAPSHOT";
    /**
     * Numeric packaging version for jpackage ({@code app.packageVersion} in pom.xml).
     * Distinct from {@link #VERSION} only because jpackage requires a numeric vendor version.
     */
    public static final String PACKAGE_VERSION = "1.0.0";
    public static final String VENDOR = "JavaBankingSystem";
    public static final String DATA_DIRECTORY_NAME = "BankingSystem";
    public static final String PACKAGE_NAME = "BankingSystem";

    private AppInfo() {
    }

    public static String displayNameWithVersion() {
        return APPLICATION_NAME + " " + VERSION;
    }

    public static String windowTitle() {
        return APPLICATION_NAME;
    }

    public static String windowTitle(String section) {
        if (section == null || section.isBlank()) {
            return APPLICATION_NAME;
        }
        return APPLICATION_NAME + " — " + section.trim();
    }
}
