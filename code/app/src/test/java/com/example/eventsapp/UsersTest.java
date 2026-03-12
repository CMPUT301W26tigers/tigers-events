package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UsersTest {
    @Test
    public void addNotification_placesNewestNotificationFirst() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );

        UserNotification first = new UserNotification(
                UserNotification.Type.WAITLISTED,
                "Event waitlisted",
                "River Valley Night Run",
                "You are still on the waitlist for River Valley Night Run."
        );
        UserNotification second = new UserNotification(
                UserNotification.Type.INVITATION,
                "Event Invitation",
                "Campus Startup Showcase",
                "You were selected from the waitlist for Campus Startup Showcase."
        );

        user.addNotification(first);
        user.addNotification(second);

        assertEquals(2, user.getNotifications().size());
        assertEquals(second, user.getNotifications().get(0));
        assertEquals(first, user.getNotifications().get(1));
    }

    @Test
    public void acceptInvitation_movesEventIntoRegisteredList() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );

        user.addInvitedEvent("Campus Startup Showcase");

        user.acceptInvitation("Campus Startup Showcase");

        assertFalse(user.getInvitedEvents().contains("Campus Startup Showcase"));
        assertTrue(user.getRegisteredEvents().contains("Campus Startup Showcase"));
    }

    @Test
    public void declineInvitation_removesPendingInviteAndTracksDecline() {
        Users user = new Users(
                "Rishawn",
                "Paramapathy",
                "rishawn@ualberta.ca",
                "Edmonton, AB",
                "Entrant",
                true
        );

        user.addInvitedEvent("Campus Startup Showcase");

        user.declineInvitation("Campus Startup Showcase");

        assertFalse(user.getInvitedEvents().contains("Campus Startup Showcase"));
        assertTrue(user.getDeclinedInvitations().contains("Campus Startup Showcase"));
    }
}
