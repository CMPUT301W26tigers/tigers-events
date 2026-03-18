package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvitationResponseControllerTest {
    @Test
    public void acceptInvitation_registersEntrantAndRemovesNotification() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );
        LotteryNotificationController lotteryController = new LotteryNotificationController();
        InvitationResponseController responseController = new InvitationResponseController();

        lotteryController.notifyChosenFromWaitlist(user, "Campus Startup Showcase");
        UserNotification notification = user.getNotifications().get(0);

        boolean handled = responseController.acceptInvitation(user, notification);

        assertTrue(handled);
        assertTrue(user.getRegisteredEvents().contains("Campus Startup Showcase"));
        assertFalse(user.getInvitedEvents().contains("Campus Startup Showcase"));
        assertFalse(user.getNotifications().contains(notification));
    }

    @Test
    public void declineInvitation_removesInviteAndTracksDeclinedEvent() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );
        LotteryNotificationController lotteryController = new LotteryNotificationController();
        InvitationResponseController responseController = new InvitationResponseController();

        lotteryController.notifyChosenFromWaitlist(user, "Campus Startup Showcase");
        UserNotification notification = user.getNotifications().get(0);

        boolean handled = responseController.declineInvitation(user, notification);

        assertTrue(handled);
        assertTrue(user.getDeclinedInvitations().contains("Campus Startup Showcase"));
        assertFalse(user.getInvitedEvents().contains("Campus Startup Showcase"));
        assertFalse(user.getNotifications().contains(notification));
    }

    @Test
    public void declineInvitation_ignoresWaitlistNotifications() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );
        LotteryNotificationController lotteryController = new LotteryNotificationController();
        InvitationResponseController responseController = new InvitationResponseController();

        lotteryController.notifyWaitlisted(user, "River Valley Night Run");
        UserNotification notification = user.getNotifications().get(0);

        boolean handled = responseController.declineInvitation(user, notification);

        assertFalse(handled);
        assertTrue(user.getWaitlistedEvents().contains("River Valley Night Run"));
        assertTrue(user.getNotifications().contains(notification));
    }
}
