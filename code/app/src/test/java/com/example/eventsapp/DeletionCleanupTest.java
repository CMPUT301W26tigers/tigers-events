package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for deletion-related logic in EventCleanupHelper.
 * Firestore operations cannot be tested without mocking, so these tests
 * focus on pure logic methods and validate the expiration calculation.
 */
public class DeletionCleanupTest {

    // ── isExpired tests (verifies the expiration window logic) ──

    @Test
    public void isExpired_eventMoreThan36HoursAgo_returnsTrue() {
        // Event date: 2026-03-01, check at 2026-03-03 (48+ hours later)
        java.util.Date checkDate = parseDate("2026-03-03 12:00");
        assertTrue(EventCleanupHelper.isExpired("2026-03-01", checkDate));
    }

    @Test
    public void isExpired_eventLessThan36HoursAgo_returnsFalse() {
        // Event date: 2026-03-01, check at 2026-03-02 06:00 (30 hours later)
        java.util.Date checkDate = parseDate("2026-03-02 06:00");
        assertFalse(EventCleanupHelper.isExpired("2026-03-01", checkDate));
    }

    @Test
    public void isExpired_futureEvent_returnsFalse() {
        java.util.Date checkDate = parseDate("2026-03-01 12:00");
        assertFalse(EventCleanupHelper.isExpired("2026-04-01", checkDate));
    }

    @Test
    public void isExpired_invalidDateFormat_returnsFalse() {
        java.util.Date checkDate = parseDate("2026-03-01 12:00");
        assertFalse(EventCleanupHelper.isExpired("not-a-date", checkDate));
    }

    @Test
    public void isExpired_emptyString_returnsFalse() {
        java.util.Date checkDate = parseDate("2026-03-01 12:00");
        assertFalse(EventCleanupHelper.isExpired("", checkDate));
    }

    @Test
    public void isExpired_exactlyAt36Hours_returnsFalse() {
        // Event date: 2026-03-01 (start of day 00:00)
        // 36 hours after start = 2026-03-02 12:00
        // Should NOT be expired at exactly 36 hours (needs to be strictly after)
        java.util.Date checkDate = parseDate("2026-03-02 12:00");
        assertFalse(EventCleanupHelper.isExpired("2026-03-01", checkDate));
    }

    @Test
    public void isExpired_justAfter36Hours_returnsTrue() {
        // 36 hours + 1 minute after start of 2026-03-01
        java.util.Date checkDate = parseDate("2026-03-02 12:01");
        assertTrue(EventCleanupHelper.isExpired("2026-03-01", checkDate));
    }

    // ── buildNotificationDocumentId (reused in deletion doc ID patterns) ──

    @Test
    public void buildNotificationDocumentId_normalInputs() {
        assertEquals("invitation_evt123",
                FirestoreNotificationHelper.buildNotificationDocumentId("invitation", "evt123"));
    }

    @Test
    public void buildNotificationDocumentId_nullType() {
        assertEquals("notification_evt123",
                FirestoreNotificationHelper.buildNotificationDocumentId(null, "evt123"));
    }

    // ── Helper ──

    private java.util.Date parseDate(String dateStr) {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CANADA)
                    .parse(dateStr);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
