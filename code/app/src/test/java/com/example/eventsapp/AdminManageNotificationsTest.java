package com.example.eventsapp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for admin notification log search/filter logic and adapter formatting.
 */
public class AdminManageNotificationsTest {

    private List<NotificationLogItem> allLogs;

    @Before
    public void setUp() {
        allLogs = new ArrayList<>();

        allLogs.add(createLog("Jane Doe", "Event Invitation",
                "You were selected from the waitlist.", "Campus BBQ", "invitation"));
        allLogs.add(createLog("John Smith", "Event waitlisted",
                "You are currently on the waitlist.", "Yoga Class", "waitlisted"));
        allLogs.add(createLog("Jane Doe", "Removed from Event",
                "You were not selected from the waitlist.", "Campus BBQ", "not_selected"));
        allLogs.add(createLog("Alice Wonder", "Private Event Invitation",
                "You have been invited to join the waiting list for a private event.",
                "VIP Gala", "private_waitlist_invitation"));
    }

    // ── Search filtering tests ──

    @Test
    public void filter_emptyQuery_returnsAllLogs() {
        List<NotificationLogItem> filtered = filterLogs("");
        assertEquals(4, filtered.size());
    }

    @Test
    public void filter_byOrganizerName_matchesCorrectLogs() {
        List<NotificationLogItem> filtered = filterLogs("jane");
        assertEquals(2, filtered.size());
        assertEquals("Jane Doe", filtered.get(0).getOrganizerName());
        assertEquals("Jane Doe", filtered.get(1).getOrganizerName());
    }

    @Test
    public void filter_byEventName_matchesCorrectLogs() {
        List<NotificationLogItem> filtered = filterLogs("campus");
        assertEquals(2, filtered.size());
    }

    @Test
    public void filter_byTitle_matchesCorrectLogs() {
        List<NotificationLogItem> filtered = filterLogs("removed");
        assertEquals(1, filtered.size());
        assertEquals("not_selected", filtered.get(0).getType());
    }

    @Test
    public void filter_byMessage_matchesCorrectLogs() {
        List<NotificationLogItem> filtered = filterLogs("private event");
        assertEquals(1, filtered.size());
        assertEquals("VIP Gala", filtered.get(0).getEventName());
    }

    @Test
    public void filter_caseInsensitive() {
        List<NotificationLogItem> filtered = filterLogs("YOGA");
        assertEquals(1, filtered.size());
        assertEquals("Yoga Class", filtered.get(0).getEventName());
    }

    @Test
    public void filter_noMatch_returnsEmpty() {
        List<NotificationLogItem> filtered = filterLogs("nonexistent");
        assertTrue(filtered.isEmpty());
    }

    @Test
    public void filter_whitespaceQuery_returnsAll() {
        List<NotificationLogItem> filtered = filterLogs("   ");
        assertEquals(4, filtered.size());
    }

    // ── Adapter formatting tests ──

    @Test
    public void formatType_invitation() {
        assertEquals("Invitation", AdminNotificationLogAdapter.formatType("invitation"));
    }

    @Test
    public void formatType_waitlisted() {
        assertEquals("Waitlisted", AdminNotificationLogAdapter.formatType("waitlisted"));
    }

    @Test
    public void formatType_notSelected() {
        assertEquals("Not Selected", AdminNotificationLogAdapter.formatType("not_selected"));
    }

    @Test
    public void formatType_privateInvite() {
        assertEquals("Private Invite", AdminNotificationLogAdapter.formatType("private_waitlist_invitation"));
    }

    @Test
    public void formatType_coOrganizerInvite() {
        assertEquals("Co-organizer Invite", AdminNotificationLogAdapter.formatType("co_organizer_invitation"));
    }

    @Test
    public void formatType_null_returnsUnknown() {
        assertEquals("Unknown", AdminNotificationLogAdapter.formatType(null));
    }

    @Test
    public void formatType_unknownType_returnsRawValue() {
        assertEquals("custom_type", AdminNotificationLogAdapter.formatType("custom_type"));
    }

    // ── buildNotificationDocumentId tests ──

    @Test
    public void buildDocId_normalTypeAndEvent() {
        assertEquals("invitation_evt1",
                FirestoreNotificationHelper.buildNotificationDocumentId("invitation", "evt1"));
    }

    @Test
    public void buildDocId_nullType_usesDefault() {
        assertEquals("notification_evt1",
                FirestoreNotificationHelper.buildNotificationDocumentId(null, "evt1"));
    }

    // ── Helpers ──

    private List<NotificationLogItem> filterLogs(String query) {
        List<NotificationLogItem> filtered = new ArrayList<>();
        String lowerQuery = query.trim().toLowerCase();

        for (NotificationLogItem log : allLogs) {
            if (lowerQuery.isEmpty()
                    || safeContains(log.getOrganizerName(), lowerQuery)
                    || safeContains(log.getEventName(), lowerQuery)
                    || safeContains(log.getTitle(), lowerQuery)
                    || safeContains(log.getMessage(), lowerQuery)) {
                filtered.add(log);
            }
        }
        return filtered;
    }

    private boolean safeContains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private NotificationLogItem createLog(String organizer, String title, String message,
                                          String eventName, String type) {
        List<Map<String, String>> recipients = new ArrayList<>();
        Map<String, String> r = new HashMap<>();
        r.put("userId", "uid_1");
        r.put("name", "Test User");
        recipients.add(r);

        return new NotificationLogItem(organizer, "org_1", title, message,
                "evt_1", eventName, type, null, recipients);
    }
}
