package com.manpreet.bank.ui;

/**
 * Formats service and validation messages as on-screen sentences, matching the
 * punctuation used by dialog and status feedback elsewhere in the application.
 */
public final class MessageText {

    private MessageText() {
    }

    public static String asSentence(String message) {
        if (message == null) {
            return "";
        }
        String trimmed = message.strip();
        if (trimmed.isEmpty()) {
            return "";
        }
        char last = trimmed.charAt(trimmed.length() - 1);
        return last == '.' || last == '!' || last == '?' ? trimmed : trimmed + ".";
    }
}
