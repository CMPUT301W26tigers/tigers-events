package com.example.eventsapp;

/**
 * Utility class for managing a global user session, often used for prototyping or 
 * maintaining a demo user state within the application.
 * This class provides static access to the current user and allows resetting to a demo state.
 */
public final class UserSession {
    private static Users currentUser = createDemoEntrant();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private UserSession() {
    }

    /**
     * Gets the current user stored in the session.
     * @return The current Users object.
     */
    public static Users getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the current user for the session.
     * @param user The Users object to set.
     */
    public static void setCurrentUser(Users user) {
        currentUser = user;
    }

    /**
     * Resets the current user to the default demo entrant profile.
     */
    public static void resetDemoUser() {
        currentUser = createDemoEntrant();
    }

    /**
     * Creates a demo user with pre-populated notifications and events for testing purposes.
     * @return A Users object initialized with demo data.
     */
    private static Users createDemoEntrant() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );

        LotteryNotificationController controller = new LotteryNotificationController();
        controller.notifyWaitlisted(user, "River Valley Night Run");
        controller.notifyChosenFromWaitlist(user, "Campus Startup Showcase");
        return user;
    }
}
