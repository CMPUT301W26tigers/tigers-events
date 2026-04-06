package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Intent tests for accepting and declining inbox invitations.
 */
public class InvitationResponseControllerIntentTest {

    @Test
    public void acceptInvitation_ignoresNonInvitationNotifications() {
        Users user = new Users("Talia", "Morgan", "talia.morgan@ualberta.ca", "Edmonton, AB", "Entrant", true);
        InvitationResponseController controller = new InvitationResponseController();
        UserNotification waitlistNotice = new UserNotification(
                UserNotification.Type.WAITLISTED,
                "Event waitlisted",
                "Design Systems Roundtable",
                "You are still on the waitlist for Design Systems Roundtable."
        );
        user.addNotification(waitlistNotice);

        boolean handled = controller.acceptInvitation(user, waitlistNotice);

        assertFalse(handled);
        assertTrue(user.getNotifications().contains(waitlistNotice));
        assertTrue(user.getRegisteredEvents().isEmpty());
    }

    @Test
    public void declineInvitation_returnsFalseForNullNotification() {
        Users user = new Users("Talia", "Morgan", "talia.morgan@ualberta.ca", "Edmonton, AB", "Entrant", true);
        InvitationResponseController controller = new InvitationResponseController();

        boolean handled = controller.declineInvitation(user, null);

        assertFalse(handled);
        assertTrue(user.getDeclinedInvitations().isEmpty());
    }
}
