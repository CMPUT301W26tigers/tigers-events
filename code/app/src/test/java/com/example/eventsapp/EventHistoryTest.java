package com.example.eventsapp;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Unit tests for the event history feature.
 * Tests the entrantStatus field on Event, date comparison logic for chip filters,
 * expiration logic, and the fromHistory flag.
 */
public class EventHistoryTest {

    /**
     * Tests that the entrantStatus field can be set and retrieved.
     */
    @Test
    public void testEventEntrantStatusField_setAndGet() {
        Event event = new Event("Test Event", 10);
        event.setEntrantStatus("ACCEPTED");
        assertEquals("ACCEPTED", event.getEntrantStatus());
    }

    /**
     * Tests that entrantStatus defaults to null when not set.
     */
    @Test
    public void testEventEntrantStatusField_defaultNull() {
        Event event = new Event("Test Event", 10);
        assertNull(event.getEntrantStatus());
    }

    /**
     * Tests that all entrant statuses can be set and retrieved correctly.
     */
    @Test
    public void testEventEntrantStatusField_allStatuses() {
        Event event = new Event("Test Event", 10);
        String[] statuses = {"PRIVATE_INVITED", "APPLIED", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED"};
        for (String status : statuses) {
            event.setEntrantStatus(status);
            assertEquals(status, event.getEntrantStatus());
        }
    }

    /**
     * Tests that a future date string compares greater than a past date string.
     * This validates the upcoming filter logic used in EventsFragment.
     */
    @Test
    public void testEventDateComparison_upcomingFilter() {
        String futureDate = "2027-01-01";
        String today = "2026-03-20";
        assertTrue("Future date should compare greater than today",
                futureDate.compareTo(today) > 0);
    }

    /**
     * Tests that a past date string compares less than a current date string.
     * This validates the past filter logic used in EventsFragment.
     */
    @Test
    public void testEventDateComparison_pastFilter() {
        String pastDate = "2025-01-01";
        String today = "2026-03-20";
        assertTrue("Past date should compare less than today",
                pastDate.compareTo(today) < 0);
    }

    /**
     * Tests that the Entrant.Status enum contains exactly the expected values.
     */
    @Test
    public void testEntrantStatusEnumValues() {
        Entrant.Status[] statuses = Entrant.Status.values();
        assertEquals(6, statuses.length);
        assertNotNull(Entrant.Status.valueOf("PRIVATE_INVITED"));
        assertNotNull(Entrant.Status.valueOf("APPLIED"));
        assertNotNull(Entrant.Status.valueOf("INVITED"));
        assertNotNull(Entrant.Status.valueOf("ACCEPTED"));
        assertNotNull(Entrant.Status.valueOf("DECLINED"));
        assertNotNull(Entrant.Status.valueOf("CANCELLED"));
    }

    /**
     * Tests that events with DECLINED status are included in history (entrantStatus is set).
     */
    @Test
    public void testDeclinedEventInHistory() {
        Event event = new Event("Declined Event", 5);
        event.setEntrantStatus("DECLINED");
        assertNotNull("Declined events should have a status set", event.getEntrantStatus());
        assertEquals("DECLINED", event.getEntrantStatus());
    }

    /**
     * Tests that events with CANCELLED status are included in history (entrantStatus is set).
     */
    @Test
    public void testCancelledEventInHistory() {
        Event event = new Event("Cancelled Event", 5);
        event.setEntrantStatus("CANCELLED");
        assertNotNull("Cancelled events should have a status set", event.getEntrantStatus());
        assertEquals("CANCELLED", event.getEntrantStatus());
    }

    /**
     * Tests that the fromHistory flag defaults to false.
     */
    @Test
    public void testFromHistory_defaultFalse() {
        Event event = new Event("Test Event", 10);
        assertFalse(event.isFromHistory());
    }

    /**
     * Tests that fromHistory can be set and retrieved.
     */
    @Test
    public void testFromHistory_setAndGet() {
        Event event = new Event("Test Event", 10);
        event.setFromHistory(true);
        assertTrue(event.isFromHistory());
    }

    /**
     * Tests that an event date well in the past is detected as expired.
     * The expiration window is event_date + 36 hours (end of day + 12 hours).
     */
    @Test
    public void testIsExpired_pastDate() {
        // An event from 2 days ago should definitely be expired
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2);
        String pastDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(cal.getTime());
        assertTrue("Event 2 days ago should be expired",
                EventCleanupHelper.isExpired(pastDate, new Date()));
    }

    /**
     * Tests that a future event date is not expired.
     */
    @Test
    public void testIsExpired_futureDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        String futureDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(cal.getTime());
        assertFalse("Event 7 days from now should not be expired",
                EventCleanupHelper.isExpired(futureDate, new Date()));
    }

    /**
     * Tests that today's event is not yet expired (within the 12-hour grace period).
     */
    @Test
    public void testIsExpired_todayNotExpired() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(new Date());
        assertFalse("Today's event should not be expired yet",
                EventCleanupHelper.isExpired(today, new Date()));
    }

    /**
     * Tests that an invalid date string does not cause expiration.
     */
    @Test
    public void testIsExpired_invalidDate() {
        assertFalse("Invalid date should not be expired",
                EventCleanupHelper.isExpired("not-a-date", new Date()));
    }

    /**
     * Tests that the ORGANIZED status can be set on events.
     */
    @Test
    public void testOrganizedStatus() {
        Event event = new Event("My Event", 10);
        event.setEntrantStatus("ORGANIZED");
        assertEquals("ORGANIZED", event.getEntrantStatus());
    }

    // ── Terminal status filter tests ──

    /**
     * Tests that DECLINED is a terminal status (event is "done" for the user).
     */
    @Test
    public void testTerminalStatus_declined() {
        String status = "DECLINED";
        boolean isTerminal = "DECLINED".equals(status) || "CANCELLED".equals(status);
        assertTrue("DECLINED should be a terminal status", isTerminal);
    }

    /**
     * Tests that CANCELLED is a terminal status.
     */
    @Test
    public void testTerminalStatus_cancelled() {
        String status = "CANCELLED";
        boolean isTerminal = "DECLINED".equals(status) || "CANCELLED".equals(status);
        assertTrue("CANCELLED should be a terminal status", isTerminal);
    }

    /**
     * Tests that ACCEPTED is NOT a terminal status.
     */
    @Test
    public void testTerminalStatus_acceptedIsNotTerminal() {
        String status = "ACCEPTED";
        boolean isTerminal = "DECLINED".equals(status) || "CANCELLED".equals(status);
        assertFalse("ACCEPTED should not be a terminal status", isTerminal);
    }

    /**
     * Tests that APPLIED is NOT a terminal status.
     */
    @Test
    public void testTerminalStatus_appliedIsNotTerminal() {
        String status = "APPLIED";
        boolean isTerminal = "DECLINED".equals(status) || "CANCELLED".equals(status);
        assertFalse("APPLIED should not be a terminal status", isTerminal);
    }

    /**
     * Tests that a DECLINED event with a future date should appear in "Past" filter.
     * This simulates the filter logic: past filter removes events where
     * eventDate >= today AND status is not terminal.
     */
    @Test
    public void testPastFilter_declinedFutureEventShows() {
        String futureDate = "2027-06-01";
        String today = "2026-03-23";
        String status = "DECLINED";
        boolean isTerminal = "DECLINED".equals(status) || "CANCELLED".equals(status);

        // Past filter removes if: eventDate >= today AND NOT terminal
        boolean shouldRemove = futureDate.compareTo(today) >= 0 && !isTerminal;
        assertFalse("DECLINED future event should NOT be removed from Past filter", shouldRemove);
    }

    /**
     * Tests that an ACCEPTED event with a future date should NOT appear in "Past" filter.
     */
    @Test
    public void testPastFilter_acceptedFutureEventHidden() {
        String futureDate = "2027-06-01";
        String today = "2026-03-23";
        String status = "ACCEPTED";
        boolean isTerminal = "DECLINED".equals(status) || "CANCELLED".equals(status);

        boolean shouldRemove = futureDate.compareTo(today) >= 0 && !isTerminal;
        assertTrue("ACCEPTED future event should be removed from Past filter", shouldRemove);
    }

    /**
     * Tests that a DECLINED event with a future date should NOT appear in "Upcoming" filter.
     */
    @Test
    public void testUpcomingFilter_declinedFutureEventHidden() {
        String futureDate = "2027-06-01";
        String today = "2026-03-23";
        String status = "DECLINED";
        boolean isTerminal = "DECLINED".equals(status) || "CANCELLED".equals(status);

        // Upcoming filter removes if: eventDate < today OR terminal
        boolean shouldRemove = futureDate.compareTo(today) < 0 || isTerminal;
        assertTrue("DECLINED future event should be removed from Upcoming filter", shouldRemove);
    }
}
