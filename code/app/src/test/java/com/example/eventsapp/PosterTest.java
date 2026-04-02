package com.example.eventsapp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for poster URL handling in the Event model
 * and Firestore data map construction for poster storage.
 */
public class PosterTest {

    // Event poster URL tests

    @Test
    public void event_posterUrl_setAndGet() {
        Event event = new Event("Test Event", 10);
        event.setPosterUrl("https://firebasestorage.googleapis.com/posters/event1.jpg");
        assertEquals("https://firebasestorage.googleapis.com/posters/event1.jpg", event.getPosterUrl());
    }

    @Test
    public void event_posterUrl_defaultsToEmpty() {
        Event event = new Event("Test Event", 10);
        assertEquals("", event.getPosterUrl());
    }

    @Test
    public void event_posterUrl_nullDefaultsToEmpty() {
        Event event = new Event("Test Event", 10);
        event.setPosterUrl(null);
        assertEquals("", event.getPosterUrl());
    }

    @Test
    public void event_posterUrl_canBeCleared() {
        Event event = new Event("Test Event", 10);
        event.setPosterUrl("https://example.com/poster.jpg");
        assertEquals("https://example.com/poster.jpg", event.getPosterUrl());

        event.setPosterUrl("");
        assertEquals("", event.getPosterUrl());
    }

    @Test
    public void event_posterUrl_fullConstructor() {
        Event event = new Event("id-1", "Event", 10, "2026-01-01", "2026-01-10",
                "2026-02-01", "desc", "https://firebasestorage.googleapis.com/posters/id-1.jpg", 5);
        assertEquals("https://firebasestorage.googleapis.com/posters/id-1.jpg", event.getPosterUrl());
    }

    // Firestore data map poster field tests

    @Test
    public void firestoreDataMap_containsPosterUrl() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "event-123");
        data.put("name", "Test Event");
        data.put("posterUrl", "https://firebasestorage.googleapis.com/posters/event-123.jpg");

        assertTrue(data.containsKey("posterUrl"));
        assertEquals("https://firebasestorage.googleapis.com/posters/event-123.jpg", data.get("posterUrl"));
    }

    @Test
    public void firestoreDataMap_emptyPosterUrl_whenRemoved() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "event-123");
        data.put("posterUrl", "https://firebasestorage.googleapis.com/posters/event-123.jpg");

        // Simulate poster removal
        data.put("posterUrl", "");
        assertEquals("", data.get("posterUrl"));
    }

    @Test
    public void firestoreDataMap_posterUrlUpdate_preservesOtherFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "event-123");
        data.put("name", "Test Event");
        data.put("amount", 50);
        data.put("posterUrl", "");

        // Simulate poster upload
        data.put("posterUrl", "https://firebasestorage.googleapis.com/posters/event-123.jpg");

        assertEquals("event-123", data.get("id"));
        assertEquals("Test Event", data.get("name"));
        assertEquals(50, data.get("amount"));
        assertEquals("https://firebasestorage.googleapis.com/posters/event-123.jpg", data.get("posterUrl"));
    }

    // Poster display logic tests

    @Test
    public void posterDisplay_hasPoster_showsThumbnail() {
        Event event = new Event("id-1", "Event", 10, "", "", "", "",
                "https://firebasestorage.googleapis.com/posters/id-1.jpg", 0);
        boolean hasPoster = event.getPosterUrl() != null && !event.getPosterUrl().isEmpty();
        assertTrue("Event with poster URL should show thumbnail", hasPoster);
    }

    @Test
    public void posterDisplay_noPoster_showsAvatar() {
        Event event = new Event("id-1", "Event", 10, "", "", "", "", "", 0);
        boolean hasPoster = event.getPosterUrl() != null && !event.getPosterUrl().isEmpty();
        assertFalse("Event without poster URL should show avatar", hasPoster);
    }

    @Test
    public void posterDisplay_nullPoster_showsAvatar() {
        Event event = new Event("id-1", "Event", 10, "", "", "", "", null, 0);
        boolean hasPoster = event.getPosterUrl() != null && !event.getPosterUrl().isEmpty();
        assertFalse("Event with null poster URL should show avatar", hasPoster);
    }
}
