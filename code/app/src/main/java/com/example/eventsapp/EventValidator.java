package com.example.eventsapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for validating event creation input.
 * All methods are static and framework-free so they can be covered by plain JUnit tests.
 */
public class EventValidator {

    /** Result codes returned by {@link #validateRegistrationPeriod}. */
    public enum PeriodResult {
        /** All dates are valid and correctly ordered. */
        VALID,
        /** One or more date strings could not be parsed (expected YYYY-MM-DD). */
        INVALID_FORMAT,
        /** Registration start date is after registration end date. */
        START_AFTER_END,
        /** Registration end date is after the event date. */
        END_AFTER_EVENT
    }

    /**
     * Validates that the registration period is internally consistent and precedes the event date.
     *
     * <p>Rules:
     * <ol>
     *   <li>All three strings must parse as valid {@code yyyy-MM-dd} dates.</li>
     *   <li>Registration start must be on or before registration end.</li>
     *   <li>Registration end must be on or before the event date.</li>
     * </ol>
     *
     * @param eventDateStr         event date string (YYYY-MM-DD)
     * @param registrationStartStr registration start date string (YYYY-MM-DD)
     * @param registrationEndStr   registration end date string (YYYY-MM-DD)
     * @return a {@link PeriodResult} indicating the validation outcome
     */
    public static PeriodResult validateRegistrationPeriod(
            String eventDateStr,
            String registrationStartStr,
            String registrationEndStr) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        sdf.setLenient(false);

        Date eventDate;
        Date registrationStart;
        Date registrationEnd;

        try {
            eventDate = sdf.parse(eventDateStr);
            registrationStart = sdf.parse(registrationStartStr);
            registrationEnd = sdf.parse(registrationEndStr);
        } catch (ParseException | NullPointerException e) {
            return PeriodResult.INVALID_FORMAT;
        }

        if (eventDate == null || registrationStart == null || registrationEnd == null) {
            return PeriodResult.INVALID_FORMAT;
        }

        if (registrationStart.after(registrationEnd)) {
            return PeriodResult.START_AFTER_END;
        }

        if (registrationEnd.after(eventDate)) {
            return PeriodResult.END_AFTER_EVENT;
        }

        return PeriodResult.VALID;
    }

    /**
     * Returns {@code true} when the event name is considered valid (non-null and not blank).
     *
     * @param name the event name to check
     * @return {@code true} if the name is non-empty after trimming
     */
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Returns {@code true} when the capacity string represents a positive integer.
     *
     * @param capacityStr the raw capacity string from user input
     * @return {@code true} if the string parses to a positive integer
     */
    public static boolean isValidCapacity(String capacityStr) {
        if (capacityStr == null || capacityStr.trim().isEmpty()) return false;
        try {
            int value = Integer.parseInt(capacityStr.trim());
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns {@code true} when the sample size string is a non-negative integer (0 is allowed).
     *
     * @param sampleStr the raw sample size string from user input
     * @return {@code true} if the string parses to a non-negative integer
     */
    public static boolean isValidSampleSize(String sampleStr) {
        if (sampleStr == null || sampleStr.trim().isEmpty()) return true;
        try {
            return Integer.parseInt(sampleStr.trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
