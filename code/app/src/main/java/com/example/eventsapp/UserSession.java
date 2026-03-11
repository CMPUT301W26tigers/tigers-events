package com.example.eventsapp;

public final class UserSession {
    private static Users currentUser = createDemoEntrant();

    private UserSession() {
    }

    public static Users getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Users user) {
        currentUser = user;
    }

    public static void resetDemoUser() {
        currentUser = createDemoEntrant();
    }

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
