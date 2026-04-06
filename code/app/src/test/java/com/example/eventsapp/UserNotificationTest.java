package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Model tests for inbox notification payloads.
 */
public class UserNotificationTest {

    @Test
    public void constructor_withFirestoreMetadata_preservesAllFields() {
        UserNotification notification = new UserNotification(
                UserNotification.Type.CO_ORGANIZER_INVITATION,
                "Co-organizer invitation",
                "Alberta Tech Week",
                "Join the organizing team for Alberta Tech Week.",
                "evt-ab-tech-week",
                "notif-coorg-17",
                "group-organizers"
        );

        assertEquals(UserNotification.Type.CO_ORGANIZER_INVITATION, notification.getType());
        assertEquals("Co-organizer invitation", notification.getTitle());
        assertEquals("Alberta Tech Week", notification.getEventName());
        assertEquals("Join the organizing team for Alberta Tech Week.", notification.getMessage());
        assertEquals("evt-ab-tech-week", notification.getEventId());
        assertEquals("notif-coorg-17", notification.getNotificationId());
        assertEquals("group-organizers", notification.getGroupId());
    }

    @Test
    public void constructor_withoutOptionalMetadata_leavesIdsNull() {
        UserNotification notification = new UserNotification(
                UserNotification.Type.GROUP_WAITLIST_INVITATION,
                "Group waitlist invite",
                "Jasper Leadership Retreat",
                "Your group has been invited to join the waitlist."
        );

        assertNull(notification.getEventId());
        assertNull(notification.getNotificationId());
        assertNull(notification.getGroupId());
    }
}
