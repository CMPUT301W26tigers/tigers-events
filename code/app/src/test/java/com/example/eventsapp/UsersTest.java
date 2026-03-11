package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
