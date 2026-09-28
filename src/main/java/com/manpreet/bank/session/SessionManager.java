package com.manpreet.bank.session;

import java.util.Optional;

/**
 * Simple in-memory session holder for the desktop application.
 */
public class SessionManager {

    private UserSession currentSession;

    public synchronized void startSession(UserSession session) {
        this.currentSession = session;
    }

    public synchronized void endSession() {
        this.currentSession = null;
    }

    public synchronized Optional<UserSession> getCurrentSession() {
        return Optional.ofNullable(currentSession);
    }

    public synchronized boolean isAuthenticated() {
        return currentSession != null;
    }
}
