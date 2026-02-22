package com.gestion.tools;

import com.gestion.entities.User;

/**
 * Session Manager - Singleton pattern
 * Stores the currently logged-in user throughout the application
 */
public class Session {

    private static Session instance;
    private User currentUser;

    // Private constructor (Singleton)
    private Session() {
    }

    // Get singleton instance
    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    // Set the logged-in user
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ Session started for: " + (user != null ? user.getName() : "null"));
    }

    // Get the current user
    public User getCurrentUser() {
        return currentUser;
    }

    // Get the current user ID
    public Integer getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    // Check if a user is logged in
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // Logout (clear session)
    public void logout() {
        System.out.println("🚪 Logging out: " + (currentUser != null ? currentUser.getName() : "null"));
        this.currentUser = null;
    }

    // Clear session
    public void clear() {
        logout();
    }
}