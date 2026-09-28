package com.manpreet.bank.session;

/**
 * Authenticated user view for the UI layer. Never includes password material.
 */
public record UserSession(
        long userId,
        String username,
        String firstName,
        String lastName,
        String email
) {
}
