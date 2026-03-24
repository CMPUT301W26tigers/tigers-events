package com.example.eventsapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link EventValidator}.
 *
 * All tests run on the local JVM — no Android framework needed.
 */
public class EventValidatorTest {

    // ───────────────────────────── validateRegistrationPeriod ─────────────────────────────

    @Test
    public void validDates_returnsValid() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-06-15", "2025-05-01", "2025-06-01");
        assertEquals(EventValidator.PeriodResult.VALID, result);
    }

    @Test
    public void sameDayStartAndEnd_returnsValid() {
        // Registration start == registration end is allowed (same-day window)
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-12-31", "2025-12-01", "2025-12-01");
        assertEquals(EventValidator.PeriodResult.VALID, result);
    }

    @Test
    public void registrationEndEqualsEventDate_returnsValid() {
        // End on the same day as the event is allowed (not strictly after)
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-09-10", "2025-08-01", "2025-09-10");
        assertEquals(EventValidator.PeriodResult.VALID, result);
    }

    @Test
    public void startAfterEnd_returnsStartAfterEnd() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-12-01", "2025-11-20", "2025-11-10");
        assertEquals(EventValidator.PeriodResult.START_AFTER_END, result);
    }

    @Test
    public void endAfterEvent_returnsEndAfterEvent() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-06-01", "2025-05-01", "2025-07-01");
        assertEquals(EventValidator.PeriodResult.END_AFTER_EVENT, result);
    }

    @Test
    public void invalidEventDateFormat_returnsInvalidFormat() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "not-a-date", "2025-05-01", "2025-06-01");
        assertEquals(EventValidator.PeriodResult.INVALID_FORMAT, result);
    }

    @Test
    public void invalidStartDateFormat_returnsInvalidFormat() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-12-01", "05/01/2025", "2025-06-01");
        assertEquals(EventValidator.PeriodResult.INVALID_FORMAT, result);
    }

    @Test
    public void invalidEndDateFormat_returnsInvalidFormat() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-12-01", "2025-05-01", "June 1 2025");
        assertEquals(EventValidator.PeriodResult.INVALID_FORMAT, result);
    }

    @Test
    public void nullEventDate_returnsInvalidFormat() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                null, "2025-05-01", "2025-06-01");
        assertEquals(EventValidator.PeriodResult.INVALID_FORMAT, result);
    }

    @Test
    public void emptyStrings_returnsInvalidFormat() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "", "", "");
        assertEquals(EventValidator.PeriodResult.INVALID_FORMAT, result);
    }

    @Test
    public void impossibleDate_feb30_returnsInvalidFormat() {
        EventValidator.PeriodResult result = EventValidator.validateRegistrationPeriod(
                "2025-02-30", "2025-01-01", "2025-01-15");
        assertEquals(EventValidator.PeriodResult.INVALID_FORMAT, result);
    }

    // ───────────────────────────── isValidName ─────────────────────────────

    @Test
    public void validName_returnsTrue() {
        assertTrue(EventValidator.isValidName("Summer Skate Event"));
    }

    @Test
    public void emptyName_returnsFalse() {
        assertFalse(EventValidator.isValidName(""));
    }

    @Test
    public void blankNameWhitespace_returnsFalse() {
        assertFalse(EventValidator.isValidName("   "));
    }

    @Test
    public void nullName_returnsFalse() {
        assertFalse(EventValidator.isValidName(null));
    }

    // ───────────────────────────── isValidCapacity ─────────────────────────────

    @Test
    public void positiveCapacity_returnsTrue() {
        assertTrue(EventValidator.isValidCapacity("50"));
    }

    @Test
    public void capacityOfOne_returnsTrue() {
        assertTrue(EventValidator.isValidCapacity("1"));
    }

    @Test
    public void zeroCapacity_returnsFalse() {
        assertFalse(EventValidator.isValidCapacity("0"));
    }

    @Test
    public void negativeCapacity_returnsFalse() {
        assertFalse(EventValidator.isValidCapacity("-5"));
    }

    @Test
    public void nonNumericCapacity_returnsFalse() {
        assertFalse(EventValidator.isValidCapacity("abc"));
    }

    @Test
    public void emptyCapacity_returnsFalse() {
        assertFalse(EventValidator.isValidCapacity(""));
    }

    @Test
    public void nullCapacity_returnsFalse() {
        assertFalse(EventValidator.isValidCapacity(null));
    }

    // ───────────────────────────── isValidSampleSize ─────────────────────────────

    @Test
    public void positiveSampleSize_returnsTrue() {
        assertTrue(EventValidator.isValidSampleSize("10"));
    }

    @Test
    public void zeroSampleSize_returnsTrue() {
        // 0 is allowed — means no lottery
        assertTrue(EventValidator.isValidSampleSize("0"));
    }

    @Test
    public void emptySampleSize_returnsTrue() {
        // Empty means not specified — treated as 0
        assertTrue(EventValidator.isValidSampleSize(""));
    }

    @Test
    public void nullSampleSize_returnsTrue() {
        assertTrue(EventValidator.isValidSampleSize(null));
    }

    @Test
    public void negativeSampleSize_returnsFalse() {
        assertFalse(EventValidator.isValidSampleSize("-1"));
    }

    @Test
    public void nonNumericSampleSize_returnsFalse() {
        assertFalse(EventValidator.isValidSampleSize("many"));
    }
}
