package com.example.eventsapp;

/**
 * Controller class responsible for managing notifications related to the event lottery process.
 * It handles notifying users when they are waitlisted, selected, or not selected for an event.
 */
public class LotteryNotificationController {

    /**
     * Notifies a user that they have been placed on the waitlist for an event.
     *
     * @param user The user to notify.
     * @param eventName The name of the event.
     */
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

    /**
     * Notifies a user that they have been selected from the waitlist for an event.
     *
     * @param user The user to notify.
     * @param eventName The name of the event.
     */
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

    /**
     * Notifies a user that they were not selected from the waitlist for an event.
     *
     * @param user The user to notify.
     * @param eventName The name of the event.
     */
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

    /**
     * Helper method to add a notification to a user's account if notifications are enabled.
     *
     * @param user The user to receive the notification.
     * @param notification The notification to add.
     */
    private void addNotification(Users user, UserNotification notification) {
        if (user.isNotificationsEnabled()) {
            user.addNotification(notification);
        }
    }
}
