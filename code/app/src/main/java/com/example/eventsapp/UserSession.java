package com.example.eventsapp;

public final class UserSession {
    private static final Users CURRENT_USER = createDemoEntrant();

    private UserSession() {
    }

    public static Users getCurrentUser() {
        return CURRENT_USER;
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
        controller.notifyNotChosenFromWaitlist(user, "Winter Food Festival");
        return user;
    }
}
