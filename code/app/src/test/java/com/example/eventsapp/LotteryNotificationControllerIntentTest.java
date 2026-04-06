package com.example.eventsapp;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Intent tests for lottery controller behavior tied to waitlist and grouped draw requirements.
 */
public class LotteryNotificationControllerIntentTest {

    @Test
    public void notifyWaitlisted_doesNotCreateInboxItemWhenNotificationsAreDisabled() {
        Users user = new Users("Liam", "Brooks", "liam.brooks@ualberta.ca", "Edmonton, AB", "Entrant", false);
        LotteryNotificationController controller = new LotteryNotificationController();

        controller.notifyWaitlisted(user, "Hackathon Finals Watch Party");

        assertTrue(user.getWaitlistedEvents().contains("Hackathon Finals Watch Party"));
        assertTrue(user.getNotifications().isEmpty());
    }

    @Test
    public void performSelection_ignoresEntrantsWhoAreNoLongerApplied() {
        LotteryNotificationController controller = new LotteryNotificationController();
        Entrant applied = new Entrant("ent-01", "evt-01", "Sara Gomez", "sara@example.com", Entrant.Status.APPLIED);
        Entrant invited = new Entrant("ent-02", "evt-01", "Ben Li", "ben@example.com", Entrant.Status.INVITED);
        Entrant accepted = new Entrant("ent-03", "evt-01", "Chloe Martin", "chloe@example.com", Entrant.Status.ACCEPTED);

        List<Entrant> selected = controller.performSelection(Arrays.asList(applied, invited, accepted), 3, 0);

        assertEquals(1, selected.size());
        assertEquals("ent-01", selected.get(0).getId());
    }

    @Test
    public void performSelection_returnsNoOneWhenAllCapacityIsAlreadyOccupied() {
        LotteryNotificationController controller = new LotteryNotificationController();
        Entrant applied = new Entrant("ent-11", "evt-11", "Jordan Reid", "jordan@example.com", Entrant.Status.APPLIED);

        List<Entrant> selected = controller.performSelection(Arrays.asList(applied), 4, 4);

        assertTrue(selected.isEmpty());
    }

    @Test
    public void performSelection_skipsFriendGroupWhenWholeGroupCannotFit() {
        LotteryNotificationController controller = new LotteryNotificationController();
        Entrant friendOne = new Entrant("grp-1", "evt-20", "Emma Clarke", "emma@example.com", Entrant.Status.APPLIED);
        Entrant friendTwo = new Entrant("grp-2", "evt-20", "Priya Nair", "priya@example.com", Entrant.Status.APPLIED);
        friendOne.setGroupId("friends-2026");
        friendTwo.setGroupId("friends-2026");

        List<Entrant> selected = controller.performSelection(Arrays.asList(friendOne, friendTwo), 1, 0);

        assertTrue(selected.isEmpty());
    }

    @Test
    public void notifyNotChosenFromWaitlist_clearsWaitlistStateBeforeAddingInboxUpdate() {
        Users user = new Users("Liam", "Brooks", "liam.brooks@ualberta.ca", "Edmonton, AB", "Entrant", true);
        LotteryNotificationController controller = new LotteryNotificationController();
        controller.notifyWaitlisted(user, "Engineering Career Fair");

        controller.notifyNotChosenFromWaitlist(user, "Engineering Career Fair");

        assertFalse(user.getWaitlistedEvents().contains("Engineering Career Fair"));
        assertEquals(UserNotification.Type.NOT_SELECTED, user.getNotifications().get(0).getType());
    }
}
