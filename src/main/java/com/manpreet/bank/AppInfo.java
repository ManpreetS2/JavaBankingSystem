package com.manpreet.bank;

/**
 * Centralized product metadata for titles, diagnostics, and packaging.
 *
 * <p>{@link #VERSION} tracks the Maven {@code project.version} in {@code pom.xml}
 * ({@code 1.0.0}). {@link #PACKAGE_VERSION} is the numeric jpackage
 * {@code app.packageVersion} and matches {@link #VERSION} for this release.
 * Keep those two pom properties and these constants in sync when bumping releases.
 * jpackage rejects a non-numeric SNAPSHOT suffix, so release versions stay numeric.
 */
public final class AppInfo {

    public static final String APPLICATION_NAME = "Banking System";
    /** Maven project.version — display version. */
    public static final String VERSION = "1.0.0";
    /**
     * Numeric packaging version for jpackage ({@code app.packageVersion} in pom.xml).
     * Matches {@link #VERSION} for the 1.0.0 release.
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
