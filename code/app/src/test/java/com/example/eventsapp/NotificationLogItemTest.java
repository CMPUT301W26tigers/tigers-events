package com.example.eventsapp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for NotificationLogItem model class.
 */
public class NotificationLogItemTest {

    private NotificationLogItem logItem;
    private List<Map<String, String>> recipients;

    @Before
    public void setUp() {
        recipients = new ArrayList<>();
        Map<String, String> r1 = new HashMap<>();
        r1.put("userId", "uid_1");
        r1.put("name", "Alice Smith");
        recipients.add(r1);

        Map<String, String> r2 = new HashMap<>();
        r2.put("userId", "uid_2");
        r2.put("name", "Bob Jones");
        recipients.add(r2);

        logItem = new NotificationLogItem(
                "Jane Doe", "org_1", "Event Invitation",
                "You were selected from the waitlist.",
                "evt_1", "Campus BBQ", "invitation",
                null, recipients
        );
    }

    @Test
    public void constructor_setsAllFields() {
        assertEquals("Jane Doe", logItem.getOrganizerName());
        assertEquals("org_1", logItem.getOrganizerId());
        assertEquals("Event Invitation", logItem.getTitle());
        assertEquals("You were selected from the waitlist.", logItem.getMessage());
        assertEquals("evt_1", logItem.getEventId());
        assertEquals("Campus BBQ", logItem.getEventName());
        assertEquals("invitation", logItem.getType());
        assertNull(logItem.getTimestamp());
        assertEquals(2, logItem.getRecipients().size());
    }

    @Test
    public void noArgConstructor_createsNonNullObject() {
        NotificationLogItem empty = new NotificationLogItem();
        assertNotNull(empty);
        assertNull(empty.getOrganizerName());
        assertNull(empty.getRecipients());
    }

    @Test
    public void getRecipientNames_returnsNames() {
        List<String> names = logItem.getRecipientNames();
        assertEquals(2, names.size());
        assertEquals("Alice Smith", names.get(0));
        assertEquals("Bob Jones", names.get(1));
    }

    @Test
    public void getRecipientNames_nullRecipients_returnsEmptyList() {
        logItem.setRecipients(null);
        List<String> names = logItem.getRecipientNames();
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }

    @Test
    public void getRecipientNames_emptyRecipients_returnsEmptyList() {
        logItem.setRecipients(new ArrayList<>());
        List<String> names = logItem.getRecipientNames();
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }

    @Test
    public void getRecipientNames_skipsEmptyNames() {
        Map<String, String> r3 = new HashMap<>();
        r3.put("userId", "uid_3");
        r3.put("name", "");
        recipients.add(r3);

        Map<String, String> r4 = new HashMap<>();
        r4.put("userId", "uid_4");
        // no name key
        recipients.add(r4);

        List<String> names = logItem.getRecipientNames();
        assertEquals(2, names.size());
    }

    @Test
    public void setters_updateFields() {
        logItem.setOrganizerName("New Organizer");
        logItem.setOrganizerId("org_2");
        logItem.setTitle("New Title");
        logItem.setMessage("New Message");
        logItem.setEventId("evt_2");
        logItem.setEventName("New Event");
        logItem.setType("waitlisted");

        assertEquals("New Organizer", logItem.getOrganizerName());
        assertEquals("org_2", logItem.getOrganizerId());
        assertEquals("New Title", logItem.getTitle());
        assertEquals("New Message", logItem.getMessage());
        assertEquals("evt_2", logItem.getEventId());
        assertEquals("New Event", logItem.getEventName());
        assertEquals("waitlisted", logItem.getType());
    }
}
