package com.manpreet.bank.ui;

import com.manpreet.bank.session.UserSession;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Read-only profile details for the Settings screen, derived from the authenticated session.
 */
public record ProfileSummary(
        String displayName,
        String firstName,
        String lastName,
        String username,
        String email
) {

    public static ProfileSummary from(UserSession session) {
        Objects.requireNonNull(session, "session must not be null");
        String firstName = clean(session.firstName());
        String lastName = clean(session.lastName());
        String username = clean(session.username());
        String fullName = Stream.of(firstName, lastName)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
        return new ProfileSummary(
                fullName.isEmpty() ? username : fullName,
                firstName,
                lastName,
                username,
                clean(session.email())
        );
    }

    public String usernameHandle() {
        return username.isEmpty() ? "" : "@" + username;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
