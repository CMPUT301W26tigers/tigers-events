package com.example.eventsapp;

public class LotteryNotificationController {
    public void notifyWaitlisted(Users user, String eventName) {
        user.addWaitlistedEvent(eventName);
        addNotification(
                user,
                new UserNotification(
                        UserNotification.Type.WAITLISTED,
                        "Event waitlisted",
                        eventName,
                        "You are still on the waitlist for " + eventName + "."
                )
        );
    }

    public void notifyChosenFromWaitlist(Users user, String eventName) {
        user.addInvitedEvent(eventName);
        addNotification(
                user,
                new UserNotification(
                        UserNotification.Type.INVITATION,
                        "Event Invitation",
                        eventName,
                        "You were selected from the waitlist for " + eventName + "."
                )
        );
    }

    public void notifyNotChosenFromWaitlist(Users user, String eventName) {
        user.removeWaitlistedEvent(eventName);
        addNotification(
                user,
                new UserNotification(
                        UserNotification.Type.NOT_SELECTED,
                        "Removed from Event",
                        eventName,
                        "You were not selected from the waitlist for " + eventName + "."
                )
        );
    }

    private void addNotification(Users user, UserNotification notification) {
        if (user.isNotificationsEnabled()) {
            user.addNotification(notification);
        }
    }
}
