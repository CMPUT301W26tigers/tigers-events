package com.example.eventsapp;

/**
 * Singleton manager class for handling the current user session and authentication state.
 * This class provides a centralized point to access, set, and clear the logged-in user.
 */
public class UserManager {
    private static UserManager instance;
    private Users currentUser;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private UserManager() {}

    /**
     * Gets the singleton instance of UserManager.
     *
     * @return The UserManager instance.
     */
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Gets the currently logged-in user.
     *
     * @return The current Users object, or null if no user is logged in.
     */
    public Users getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the current user upon login or sign-up.
     *
     * @param user The Users object to set as the current user.
     */
    public void setCurrentUser(Users user) {
        this.currentUser = user;
    }

    /**
     * Checks if a user is currently logged into the application.
     *
     * @return True if a user is logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Logs out the current user by clearing the session.
     */
    public void logout() {
        currentUser = null;
    }
}
