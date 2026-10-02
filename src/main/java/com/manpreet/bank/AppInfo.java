package com.manpreet.bank;

/**
 * Centralized product metadata for titles, diagnostics, and packaging.
 * Version tracks the Maven project version in {@code pom.xml}.
 */
public final class AppInfo {

    public static final String APPLICATION_NAME = "Banking System";
    public static final String VERSION = "1.0-SNAPSHOT";
    public static final String VENDOR = "JavaBankingSystem";
    public static final String DATA_DIRECTORY_NAME = "BankingSystem";

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
