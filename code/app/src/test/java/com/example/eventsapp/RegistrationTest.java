package com.example.eventsapp;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

public class RegistrationTest {

    private boolean isValidRegistrationPeriod(String eventDateStr,
                                              String registrationStartStr,
                                              String registrationEndStr) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        sdf.setLenient(false);

        try {
            Date eventDate = sdf.parse(eventDateStr);
            Date registrationStart = sdf.parse(registrationStartStr);
            Date registrationEnd = sdf.parse(registrationEndStr);

            if (eventDate == null || registrationStart == null || registrationEnd == null) {
                return false;
            }

            if (registrationStart.after(registrationEnd)) {
                return false;
            }

            if (registrationEnd.after(eventDate)) {
                return false;
            }

        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    @Test
    public void validRegistrationPeriod_returnsTrue() {
        assertTrue(isValidRegistrationPeriod(
                "2026-05-10",
                "2026-04-20",
                "2026-04-30"
        ));
    }

    @Test
    public void registrationStartAfterEnd_returnsFalse() {
        assertFalse(isValidRegistrationPeriod(
                "2026-05-01",
                "2026-04-25",
                "2026-04-10"
        ));
    }

    @Test
    public void registrationEndAfterEventDate_returnsFalse() {
        assertFalse(isValidRegistrationPeriod(
                "2026-05-01",
                "2026-04-01",
                "2026-05-05"
        ));
    }

    @Test
    public void equalStartAndEnd_isValid() {
        assertTrue(isValidRegistrationPeriod(
                "2026-06-01",
                "2026-04-20",
                "2026-04-20"
        ));
    }

    @Test
    public void registrationEndEqualEventDate_isValid() {
        assertTrue(isValidRegistrationPeriod(
                "2026-05-01",
                "2026-04-01",
                "2026-05-01"
        ));
    }

    @Test
    public void invalidDateFormat_returnsFalse() {
        assertFalse(isValidRegistrationPeriod(
                "05-01-2026",
                "04-01-2026",
                "04-20-2026"
        ));
    }

    @Test
    public void impossibleDate_returnsFalse() {
        assertFalse(isValidRegistrationPeriod(
                "2026-04-31",
                "2026-04-01",
                "2026-04-20"
        ));
    }
}