package com.manpreet.bank.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SessionManagerTest {

    @Test
    void loginCreatesSessionAndLogoutClearsIt() {
        SessionManager sessions = new SessionManager();
        assertFalse(sessions.isAuthenticated());
        assertTrue(sessions.getCurrentSession().isEmpty());

        UserSession session = new UserSession(9L, "ada", "Ada", "Lovelace", "ada@example.com");
        sessions.startSession(session);

        assertTrue(sessions.isAuthenticated());
        assertEquals(session, sessions.getCurrentSession().orElseThrow());

        sessions.endSession();
        assertFalse(sessions.isAuthenticated());
        assertTrue(sessions.getCurrentSession().isEmpty());
    }

    @Test
    void userSessionNeverCarriesPasswordMaterial() {
        Set<String> components = Arrays.stream(UserSession.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("userId", "username", "firstName", "lastName", "email"), components);
        for (String name : components) {
            String lower = name.toLowerCase(Locale.ROOT);
            assertFalse(lower.contains("password") || lower.contains("hash"),
                    "UserSession must not expose credential fields: " + name);
        }

        UserSession session = new UserSession(1L, "ada", "Ada", "Lovelace", "ada@example.com");
        String rendered = session.toString();
        assertFalse(rendered.toLowerCase(Locale.ROOT).contains("password"));
        assertFalse(rendered.contains("pbkdf2"));
    }
}
