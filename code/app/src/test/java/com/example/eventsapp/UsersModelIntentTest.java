package com.example.eventsapp;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Intent tests for user model transitions around invitations, waitlists, and inbox state.
 */
public class UsersModelIntentTest {

    @Test
    public void addInvitedEvent_removesDuplicateWaitlistState() {
        Users user = new Users("Avery", "Chen", "avery.chen@ualberta.ca", "Edmonton, AB", "Entrant", true);

        user.addWaitlistedEvent("Campus Startup Showcase");
        user.addInvitedEvent("Campus Startup Showcase");

        assertFalse(user.getWaitlistedEvents().contains("Campus Startup Showcase"));
        assertEquals(1, user.getInvitedEvents().size());
        assertTrue(user.getInvitedEvents().contains("Campus Startup Showcase"));
    }

    @Test
    public void acceptInvitation_doesNotDuplicateRegisteredEvent() {
        Users user = new Users("Noah", "Singh", "noah.singh@ualberta.ca", "Edmonton, AB", "Entrant", true);

        user.addInvitedEvent("Dean's Speaker Series");
        user.acceptInvitation("Dean's Speaker Series");
        user.acceptInvitation("Dean's Speaker Series");

        assertEquals(1, user.getRegisteredEvents().size());
    }

    @Test
    public void getNotifications_returnsReadOnlyView() {
        Users user = new Users("Maya", "Patel", "maya.patel@ualberta.ca", "Edmonton, AB", "Entrant", true);
        user.addNotification(new UserNotification(
                UserNotification.Type.WAITLISTED,
                "Event waitlisted",
                "River Valley Night Run",
                "You are still on the waitlist for River Valley Night Run."
        ));

        try {
            user.getNotifications().add(new UserNotification(
                    UserNotification.Type.NOT_SELECTED,
                    "Removed from Event",
                    "River Valley Night Run",
                    "You were not selected from the waitlist for River Valley Night Run."
            ));
            fail("Notifications list should be read-only");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, user.getNotifications().size());
        }
    }

    @Test
    public void setNotifications_acceptsFirestoreHydratedList() {
        Users user = new Users("Maya", "Patel", "maya.patel@ualberta.ca", "Edmonton, AB", "Entrant", true);
        ArrayList<UserNotification> restored = new ArrayList<>();
        restored.add(new UserNotification(
                UserNotification.Type.INVITATION,
                "Event Invitation",
                "Prairie Product Meetup",
                "You were selected from the waitlist for Prairie Product Meetup.",
                "evt-prairie-42",
                "notif-1001"
        ));

        user.setNotifications(restored);

        assertEquals(1, user.getNotifications().size());
        assertEquals("evt-prairie-42", user.getNotifications().get(0).getEventId());
    }
}
