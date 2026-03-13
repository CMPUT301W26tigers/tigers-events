package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

    // Sign-up constructor tests

    @Test
    public void signUpConstructor_setsAllFieldsCorrectly() {
        Users user = new Users("Yao", "Ming", "Yao@ming.com", "pass123", "7801234567", "device-abc");

        assertEquals("Yao", user.getFirstName());
        assertEquals("Ming", user.getLastName());
        assertEquals("Yao Ming", user.getName());
        assertEquals("Yao@ming.com", user.getEmail());
        assertEquals("pass123", user.getPassword());
        assertEquals("7801234567", user.getPhoneNumber());
        assertEquals("device-abc", user.getDeviceId());
        assertEquals("Entrant", user.getAccountType());
    }

    // Sign-in constructor tests

    @Test
    public void signInConstructor_setsFieldsCorrectly() {
        Users user = new Users("Doug Dimmadome", "DDD@DDD.com", "securePass", "1987654321");

        assertEquals("Doug Dimmadome", user.getName());
        assertEquals("DDD@DDD.com", user.getEmail());
        assertEquals("securePass", user.getPassword());
        assertEquals("1987654321", user.getPhoneNumber());
    }

    // Login tests

    @Test
    public void login_correctCredentials_returnsTrue() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        assertTrue(user.login("test@test.com", "mypass"));
    }

    @Test
    public void login_wrongPassword_returnsFalse() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        assertFalse(user.login("test@test.com", "wrongpass"));
    }

    @Test
    public void login_wrongEmail_returnsFalse() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        assertFalse(user.login("wrong@test.com", "mypass"));
    }

    @Test
    public void login_nullEmail_returnsFalse() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        assertFalse(user.login(null, "mypass"));
    }

    @Test
    public void login_nullPassword_returnsFalse() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        assertFalse(user.login("test@test.com", null));
    }

    // Device ID tests

    @Test
    public void setDeviceId_setsAndGetsCorrectly() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        user.setDeviceId("device-123");
        assertEquals("device-123", user.getDeviceId());
    }

    @Test
    public void setDeviceId_null_clearsDeviceId() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        user.setDeviceId("device-123");
        user.setDeviceId(null);
        assertNull(user.getDeviceId());
    }

    // Phone number as String test

    @Test
    public void phoneNumber_handlesStringFormat() {
        Users user = new Users("Test User", "test@test.com", "mypass", "5551234567");
        assertEquals("5551234567", user.getPhoneNumber());
        user.setPhoneNumber("7809876543");
        assertEquals("7809876543", user.getPhoneNumber());
    }
}
