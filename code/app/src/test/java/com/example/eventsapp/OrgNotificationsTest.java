package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for NotificationItem objects used in organizer notifications.
 */
public class OrgNotificationsTest {

    @Test
    public void constructor_setsFieldsCorrectly() {

        NotificationItem notification = new NotificationItem(
                "You've been selected!",
                "You were chosen to sign up for this event.",
                "goodevent",
                "Good Event",
                "invitation",
                false
        );

        assertEquals("You've been selected!", notification.getTitle());
        assertEquals("You were chosen to sign up for this event.", notification.getMessage());
        assertEquals("goodevent", notification.getEventId());
        assertEquals("Good Event", notification.getEventName());
        assertEquals("invitation", notification.getType());
        assertFalse(notification.isRead());
    }

    @Test
    public void emptyConstructor_createsObject() {

        NotificationItem notification = new NotificationItem();

        assertNotNull(notification);
    }

    @Test
    public void setTitle_updatesTitle() {

        NotificationItem notification = new NotificationItem();
        notification.setTitle("New Title");

        assertEquals("New Title", notification.getTitle());
    }

    @Test
    public void setMessage_updatesMessage() {

        NotificationItem notification = new NotificationItem();
        notification.setMessage("New Message");

        assertEquals("New Message", notification.getMessage());
    }

    @Test
    public void setEventId_updatesEventId() {

        NotificationItem notification = new NotificationItem();
        notification.setEventId("funevent");

        assertEquals("funevent", notification.getEventId());
    }

    @Test
    public void setType_updatesType() {

        NotificationItem notification = new NotificationItem();
        notification.setType("lottery");

        assertEquals("lottery", notification.getType());
    }

    @Test
    public void setRead_updatesReadStatus() {

        NotificationItem notification = new NotificationItem();
        notification.setRead(true);

        assertTrue(notification.isRead());
    }
}
