package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LotteryNotificationControllerTest {
    @Test
    public void notifyChosenFromWaitlist_createsInvitationNotification() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );
        LotteryNotificationController controller = new LotteryNotificationController();

        controller.notifyChosenFromWaitlist(user, "Campus Startup Showcase");

        UserNotification notification = user.getNotifications().get(0);
        assertEquals(UserNotification.Type.INVITATION, notification.getType());
        assertEquals("Event Invitation", notification.getTitle());
        assertTrue(notification.getMessage().contains("Campus Startup Showcase"));
    }

    @Test
    public void notifyNotChosenFromWaitlist_createsRemovalNotification() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );
        LotteryNotificationController controller = new LotteryNotificationController();

        controller.notifyNotChosenFromWaitlist(user, "Winter Food Festival");

        UserNotification notification = user.getNotifications().get(0);
        assertEquals(UserNotification.Type.NOT_SELECTED, notification.getType());
        assertEquals("Removed from Event", notification.getTitle());
        assertTrue(notification.getMessage().contains("Winter Food Festival"));
    }
}
