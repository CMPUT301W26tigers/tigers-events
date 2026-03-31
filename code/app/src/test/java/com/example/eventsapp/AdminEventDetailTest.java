package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the Event model logic used in AdminEventDetailFragment.
 * Covers display formatting, poster URL checks, and host ID handling.
 */
public class AdminEventDetailTest {

    // Event display fields

    @Test
    public void event_allFields_displaysCorrectValues() {
        Event event = new Event("ev1", "Meet comparison competition", 50,
                "2026-03-01", "2026-03-15", "2026-03-27",
                "Meet and greet for mr.comparison competition", "http://poster.jpg", 10);

        assertEquals("Meet comparison competition", event.getName());
        assertEquals(50, event.getAmount());
        assertEquals("2026-03-01", event.getRegistration_start());
        assertEquals("2026-03-15", event.getRegistration_end());
        assertEquals("2026-03-27", event.getEvent_date());
        assertEquals("Meet and greet for mr.comparison competition", event.getDescription());
        assertEquals("http://poster.jpg", event.getPosterUrl());
        assertEquals(10, event.getSampleSize());
    }

    // Formatted date tests

    @Test
    public void event_formattedDate_displaysCorrectly() {
        Event event = new Event("ev1", "Test", 10,
                "", "", "2026-03-27", "", "", 0);

        assertEquals("March 27, 2026", event.getFormattedEventDate());
    }

    @Test
    public void event_emptyDate_showsNoDate() {
        Event event = new Event("ev1", "Test", 10,
                "", "", "", "", "", 0);

        assertEquals("No date", event.getFormattedEventDate());
    }

    @Test
    public void event_nullDate_showsNoDate() {
        Event event = new Event("Test", 5);
        assertEquals("No date", event.getFormattedEventDate());
    }

    // Poster URL visibility logic (mirrors AdminEventDetailFragment behavior)

    @Test
    public void event_withPosterUrl_deletePosterShouldBeVisible() {
        Event event = new Event("ev1", "Test", 10,
                "", "", "", "", "http://poster.jpg", 0);
        String posterUrl = event.getPosterUrl();
        boolean showDeletePoster = posterUrl != null && !posterUrl.isEmpty();
        assertTrue(showDeletePoster);
    }

    @Test
    public void event_emptyPosterUrl_deletePosterShouldBeHidden() {
        Event event = new Event("ev1", "Test", 10,
                "", "", "", "", "", 0);
        String posterUrl = event.getPosterUrl();
        boolean showDeletePoster = posterUrl != null && !posterUrl.isEmpty();
        assertFalse(showDeletePoster);
    }

    @Test
    public void event_nullPosterUrl_deletePosterShouldBeHidden() {
        Event event = new Event("ev1", "Test", 10,
                "", "", "", "", null, 0);
        String posterUrl = event.getPosterUrl();
        // Constructor converts null to ""
        boolean showDeletePoster = posterUrl != null && !posterUrl.isEmpty();
        assertFalse(showDeletePoster);
    }

    // Host ID tests

    @Test
    public void event_setHostId_getsCorrectly() {
        Event event = new Event("Test", 5);
        event.setHostId("host-123");
        assertEquals("host-123", event.getHostId());
    }

    @Test
    public void event_nullHostId_returnsNull() {
        Event event = new Event("Test", 5);
        // hostId is not set, should be null
        assertEquals(null, event.getHostId());
    }

    // Host name resolution logic (mirrors AdminEventDetailFragment behavior)

    @Test
    public void event_emptyHostId_showsUnknown() {
        Event event = new Event("Test", 5);
        event.setHostId("");
        String hostId = event.getHostId();
        boolean shouldFetchHost = hostId != null && !hostId.isEmpty();
        assertFalse(shouldFetchHost);
    }

    // Description fallback

    @Test
    public void event_emptyDescription_showsFallback() {
        Event event = new Event("ev1", "Test", 10,
                "", "", "", "", "", 0);
        String desc = event.getDescription();
        String display = (desc != null && !desc.isEmpty()) ? desc : "No description provided.";
        assertEquals("No description provided.", display);
    }

    @Test
    public void event_withDescription_showsDescription() {
        Event event = new Event("ev1", "Test", 10,
                "", "", "", "A great event", "", 0);
        String desc = event.getDescription();
        String display = (desc != null && !desc.isEmpty()) ? desc : "No description provided.";
        assertEquals("A great event", display);
    }

    // Registration window display

    @Test
    public void event_registrationWindow_formatsCorrectly() {
        Event event = new Event("ev1", "Test", 10,
                "2026-03-01", "2026-03-15", "", "", "", 0);
        String regWindow = event.getRegistration_start() + " → " + event.getRegistration_end();
        assertEquals("2026-03-01 → 2026-03-15", regWindow);
    }

    // Geolocation field (from main branch merge)

    @Test
    public void event_geolocationRequired_defaultsFalse() {
        Event event = new Event("Test", 5);
        assertFalse(event.isGeolocationRequired());
    }

    @Test
    public void event_setGeolocationRequired_getsCorrectly() {
        Event event = new Event("Test", 5);
        event.setGeolocationRequired(true);
        assertTrue(event.isGeolocationRequired());
    }
}
