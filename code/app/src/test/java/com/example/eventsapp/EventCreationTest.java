package com.example.eventsapp;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for event creation, event model functionality, entrant lifecycle,
 * and the event history/expiration system.
 */
public class EventCreationTest {

    //  Event Construction

    @Test
    public void createEvent_basicConstructor_setsNameAndAmount() {
        Event event = new Event("4.7 legged race", 20);
        assertEquals("4.7 legged race", event.getName());
        assertEquals(20, event.getAmount());
    }

    @Test
    public void createEvent_basicConstructor_generatesId() {
        Event event = new Event("Test Event", 10);
        assertNotNull("Event ID should be auto-generated", event.getId());
        assertFalse("Event ID should not be empty", event.getId().isEmpty());
    }

    @Test
    public void createEvent_basicConstructor_defaultsEmptyDates() {
        Event event = new Event("Test Event", 10);
        assertEquals("", event.getEvent_date());
        assertEquals("", event.getRegistration_start());
        assertEquals("", event.getRegistration_end());
    }

    @Test
    public void createEvent_fullConstructor_allFieldsSet() {
        Event event = new Event("event-123", "Soccer Match", 50,
                "2026-04-01", "2026-04-10", "2026-05-01",
                "A fun soccer match", "https://example.com/poster.jpg", 10);
        assertEquals("event-123", event.getId());
        assertEquals("Soccer Match", event.getName());
        assertEquals(50, event.getAmount());
        assertEquals("2026-04-01", event.getRegistration_start());
        assertEquals("2026-04-10", event.getRegistration_end());
        assertEquals("2026-05-01", event.getEvent_date());
        assertEquals("A fun soccer match", event.getDescription());
        assertEquals("https://example.com/poster.jpg", event.getPosterUrl());
        assertEquals(10, event.getSampleSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createEvent_zeroAmount_throwsException() {
        new Event("Bad Event", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createEvent_fullConstructor_zeroAmount_throwsException() {
        new Event("id", "Name", 0, "", "", "", "", "", 0);
    }

    @Test
    public void createEvent_nullDescription_defaultsToEmpty() {
        Event event = new Event("id", "Name", 10, null, null, null, null, null, 0);
        assertEquals("", event.getDescription());
        assertEquals("", event.getPosterUrl());
        assertEquals("", event.getEvent_date());
        assertEquals("", event.getRegistration_start());
        assertEquals("", event.getRegistration_end());
    }

    @Test
    public void createEvent_nullId_generatesUUID() {
        Event event = new Event(null, "Name", 10, "", "", "", "", "", 0);
        assertNotNull(event.getId());
        assertFalse(event.getId().isEmpty());
    }

    @Test
    public void createEvent_twoEvents_haveDifferentIds() {
        Event event1 = new Event("Event A", 10);
        Event event2 = new Event("Event B", 10);
        assertNotEquals("Two events should have different IDs", event1.getId(), event2.getId());
    }

    //  Event Setters

    @Test
    public void setAmount_validAmount_succeeds() {
        Event event = new Event("Test", 10);
        event.setAmount(50);
        assertEquals(50, event.getAmount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAmount_zero_throwsException() {
        Event event = new Event("Test", 10);
        event.setAmount(0);
    }

    @Test
    public void setSampleSize_negative_clampsToZero() {
        Event event = new Event("Test", 10);
        event.setSampleSize(-5);
        assertEquals(0, event.getSampleSize());
    }

    @Test
    public void setSampleSize_positive_setsValue() {
        Event event = new Event("Test", 10);
        event.setSampleSize(25);
        assertEquals(25, event.getSampleSize());
    }

    @Test
    public void setDescription_null_defaultsToEmpty() {
        Event event = new Event("Test", 10);
        event.setDescription(null);
        assertEquals("", event.getDescription());
    }

    @Test
    public void setPosterUrl_null_defaultsToEmpty() {
        Event event = new Event("Test", 10);
        event.setPosterUrl(null);
        assertEquals("", event.getPosterUrl());
    }

    @Test
    public void setEventDate_null_defaultsToEmpty() {
        Event event = new Event("Test", 10);
        event.setEvent_date(null);
        assertEquals("", event.getEvent_date());
    }


    //  Entrant Creation & Status

    @Test
    public void createEntrant_defaultsToApplied() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", null);
        assertEquals(Entrant.Status.APPLIED, entrant.getStatus());
    }

    @Test
    public void createEntrant_nullId_generatesUUID() {
        Entrant entrant = new Entrant(null, "event1", "Bob", "bob@test.com", Entrant.Status.APPLIED);
        assertNotNull(entrant.getId());
        assertFalse(entrant.getId().isEmpty());
    }

    @Test
    public void createEntrant_nullName_defaultsToEmpty() {
        Entrant entrant = new Entrant("e1", "event1", null, null, Entrant.Status.APPLIED);
        assertEquals("", entrant.getName());
        assertEquals("", entrant.getEmail());
    }

    @Test
    public void entrant_statusTransition_appliedToInvited() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", Entrant.Status.APPLIED);
        assertFalse("APPLIED entrant should not be chosen", entrant.isChosenInvited());
        entrant.setStatus(Entrant.Status.INVITED);
        assertTrue("INVITED entrant should be chosen", entrant.isChosenInvited());
    }

    @Test
    public void entrant_statusTransition_invitedToAccepted() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", Entrant.Status.INVITED);
        assertTrue(entrant.isChosenInvited());
        entrant.setStatus(Entrant.Status.ACCEPTED);
        assertTrue("ACCEPTED entrant should still be chosen", entrant.isChosenInvited());
    }

    @Test
    public void entrant_statusTransition_invitedToDeclined() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", Entrant.Status.INVITED);
        entrant.setStatus(Entrant.Status.DECLINED);
        assertFalse("DECLINED entrant should not be chosen", entrant.isChosenInvited());
    }

    @Test
    public void entrant_statusTransition_appliedToCancelled() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", Entrant.Status.APPLIED);
        entrant.setStatus(Entrant.Status.CANCELLED);
        assertFalse("CANCELLED entrant should not be chosen", entrant.isChosenInvited());
    }

    @Test
    public void entrant_userId_setAndGet() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", Entrant.Status.APPLIED);
        assertNull(entrant.getUserId());
        entrant.setUserId("user-123");
        assertEquals("user-123", entrant.getUserId());
    }

    @Test
    public void entrant_statusCode_matchesStatus() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", Entrant.Status.APPLIED);
        entrant.setStatusCode(4);
        assertEquals(4, entrant.getStatusCode());

        entrant.setStatusCode(0);
        assertEquals(0, entrant.getStatusCode());

        entrant.setStatusCode(1);
        assertEquals(1, entrant.getStatusCode());

        entrant.setStatusCode(2);
        assertEquals(2, entrant.getStatusCode());

        entrant.setStatusCode(3);
        assertEquals(3, entrant.getStatusCode());
    }

    @Test
    public void entrant_privateInvitationStatus_isPendingUntilAccepted() {
        Entrant entrant = new Entrant("e1", "event1", "Alice", "alice@test.com", Entrant.Status.PRIVATE_INVITED);
        entrant.setStatusCode(4);

        assertEquals(Entrant.Status.PRIVATE_INVITED, entrant.getStatus());
        assertEquals(4, entrant.getStatusCode());
        assertFalse(entrant.isChosenInvited());
    }

    //  Event History Fields

    @Test
    public void eventHistory_entrantStatus_allValues() {
        Event event = new Event("Test", 10);
        String[] allStatuses = {"APPLIED", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED", "ORGANIZED"};
        for (String status : allStatuses) {
            event.setEntrantStatus(status);
            assertEquals(status, event.getEntrantStatus());
        }
    }

    @Test
    public void eventHistory_fromHistory_defaultFalse() {
        Event event = new Event("Test", 10);
        assertFalse(event.isFromHistory());
    }

    @Test
    public void eventHistory_fromHistory_canBeToggled() {
        Event event = new Event("Test", 10);
        event.setFromHistory(true);
        assertTrue(event.isFromHistory());
        event.setFromHistory(false);
        assertFalse(event.isFromHistory());
    }

    // Expiration Logic

    @Test
    public void expiration_eventTwoDaysAgo_isExpired() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        String pastDate = formatDate(cal.getTime());
        assertTrue(EventCleanupHelper.isExpired(pastDate, new Date()));
    }

    @Test
    public void expiration_eventNextWeek_isNotExpired() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        String futureDate = formatDate(cal.getTime());
        assertFalse(EventCleanupHelper.isExpired(futureDate, new Date()));
    }

    @Test
    public void expiration_eventToday_isNotExpired() {
        String today = formatDate(new Date());
        assertFalse(EventCleanupHelper.isExpired(today, new Date()));
    }

    @Test
    public void expiration_eventYesterday_withinGracePeriod() {
        // Yesterday's event: end of day was ~24h ago, but 12h grace = 36h total
        // So an event yesterday at 00:00 needs current time > yesterday 00:00 + 36h
        // This depends on current time of day, so test with a controlled "now"
        Calendar eventCal = Calendar.getInstance();
        eventCal.set(2026, Calendar.MARCH, 20); // March 20

        // 30 hours after start of March 20 = still within 36h grace
        Calendar now30h = Calendar.getInstance();
        now30h.set(2026, Calendar.MARCH, 21, 6, 0); // March 21 at 6am
        assertFalse("30h after event start should not be expired",
                EventCleanupHelper.isExpired("2026-03-20", now30h.getTime()));

        // 40 hours after start of March 20 = past 36h grace
        Calendar now40h = Calendar.getInstance();
        now40h.set(2026, Calendar.MARCH, 21, 16, 0); // March 21 at 4pm
        assertTrue("40h after event start should be expired",
                EventCleanupHelper.isExpired("2026-03-20", now40h.getTime()));
    }

    @Test
    public void expiration_invalidDate_notExpired() {
        assertFalse(EventCleanupHelper.isExpired("invalid", new Date()));
        assertFalse(EventCleanupHelper.isExpired("", new Date()));
        assertFalse(EventCleanupHelper.isExpired("2026-13-01", new Date()));
    }

    //  History Data Structure

    @Test
    public void historyData_containsAllRequiredFields() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", "event-123");
        eventData.put("name", "Pool Party");
        eventData.put("amount", 30);
        eventData.put("description", "A pool party event");
        eventData.put("event_date", "2026-07-15");
        eventData.put("registration_start", "2026-06-01");
        eventData.put("registration_end", "2026-07-01");
        eventData.put("posterUrl", "https://example.com/poster.jpg");
        eventData.put("sampleSize", 10);

        // Simulate what writeHistoryRecord does
        Map<String, Object> historyData = new HashMap<>(eventData);
        historyData.put("entrantStatus", "APPLIED");
        historyData.put("expired", false);

        assertEquals("event-123", historyData.get("id"));
        assertEquals("Pool Party", historyData.get("name"));
        assertEquals(30, historyData.get("amount"));
        assertEquals("APPLIED", historyData.get("entrantStatus"));
        assertEquals(false, historyData.get("expired"));
        assertEquals("2026-07-15", historyData.get("event_date"));
    }

    @Test
    public void historyData_statusUpdate_preservesOtherFields() {
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("id", "event-123");
        historyData.put("name", "Pool Party");
        historyData.put("entrantStatus", "APPLIED");
        historyData.put("expired", false);

        // Simulate status update (only entrantStatus changes)
        historyData.put("entrantStatus", "INVITED");

        assertEquals("event-123", historyData.get("id"));
        assertEquals("Pool Party", historyData.get("name"));
        assertEquals("INVITED", historyData.get("entrantStatus"));
        assertEquals(false, historyData.get("expired"));
    }

    @Test
    public void historyData_expiration_setsExpiredTrue() {
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("entrantStatus", "ACCEPTED");
        historyData.put("expired", false);

        // Simulate what cleanup does
        historyData.put("expired", true);

        assertTrue((Boolean) historyData.get("expired"));
        assertEquals("ACCEPTED", historyData.get("entrantStatus"));
    }

    // Entrant Full Lifecycle

    @Test
    public void entrantLifecycle_applyThroughAccept() {
        // Simulate the full lifecycle of an entrant
        Event event = new Event("Vegan Chili Contest", 50);
        event.setSampleSize(10);

        Entrant entrant = new Entrant(null, event.getId(), "Charlie", "charlie@test.com", Entrant.Status.APPLIED);
        entrant.setUserId("user-charlie");
        entrant.setStatusCode(0);

        // Step 1: User applies
        assertEquals(Entrant.Status.APPLIED, entrant.getStatus());
        assertEquals(0, entrant.getStatusCode());
        assertFalse(entrant.isChosenInvited());

        // Step 2: Lottery selects user
        entrant.setStatus(Entrant.Status.INVITED);
        entrant.setStatusCode(1);
        assertEquals(Entrant.Status.INVITED, entrant.getStatus());
        assertTrue(entrant.isChosenInvited());

        // Step 3: User accepts invitation
        entrant.setStatus(Entrant.Status.ACCEPTED);
        entrant.setStatusCode(2);
        assertEquals(Entrant.Status.ACCEPTED, entrant.getStatus());
        assertTrue(entrant.isChosenInvited());
    }

    @Test
    public void entrantLifecycle_applyThroughDecline() {
        Entrant entrant = new Entrant(null, "event1", "Dana", "dana@test.com", Entrant.Status.APPLIED);
        entrant.setStatusCode(0);

        // Lottery selects
        entrant.setStatus(Entrant.Status.INVITED);
        entrant.setStatusCode(1);

        // User declines
        entrant.setStatus(Entrant.Status.DECLINED);
        entrant.setStatusCode(3);
        assertEquals(Entrant.Status.DECLINED, entrant.getStatus());
        assertFalse(entrant.isChosenInvited());
    }

    @Test
    public void entrantLifecycle_applyThenCancelled() {
        Entrant entrant = new Entrant(null, "event1", "Eve", "eve@test.com", Entrant.Status.APPLIED);

        // Organizer cancels
        entrant.setStatus(Entrant.Status.CANCELLED);
        entrant.setStatusCode(3);
        assertEquals(Entrant.Status.CANCELLED, entrant.getStatus());
        assertFalse(entrant.isChosenInvited());
    }

    // Event with History Context

    @Test
    public void eventFromHistory_hasStatusAndFlag() {
        Event event = new Event("event-old", "Past Concert", 100,
                "2025-01-01", "2025-01-10", "2025-02-01",
                "A great concert", "", 20);
        event.setEntrantStatus("ACCEPTED");
        event.setFromHistory(true);

        assertTrue(event.isFromHistory());
        assertEquals("ACCEPTED", event.getEntrantStatus());
        assertEquals("Past Concert", event.getName());
    }

    @Test
    public void eventNotFromHistory_defaultBehavior() {
        Event event = new Event("Active Event", 30);
        event.setEntrantStatus("APPLIED");

        assertFalse(event.isFromHistory());
        assertEquals("APPLIED", event.getEntrantStatus());
    }

    // Helper

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(date);
    }
}
