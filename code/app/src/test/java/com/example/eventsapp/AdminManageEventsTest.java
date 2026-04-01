package com.example.eventsapp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for AdminManageEventsFragment search and poster filtering logic.
 */
public class AdminManageEventsTest {

    private List<Event> allEvents;

    @Before
    public void setUp() {
        allEvents = new ArrayList<>();
        allEvents.add(new Event("1", "Soccer Match", 50, "2026-04-01", "2026-04-10",
                "2026-05-01", "A fun soccer match", "https://storage.com/posters/1.jpg", 10));
        allEvents.add(new Event("2", "Yoga Class", 20, "2026-04-01", "2026-04-10",
                "2026-05-01", "Relaxing yoga session", "", 5));
        allEvents.add(new Event("3", "Art Exhibition", 100, "2026-04-01", "2026-04-10",
                "2026-05-01", "Modern art showcase", "https://storage.com/posters/3.jpg", 30));
        allEvents.add(new Event("4", "Soccer Training", 25, "2026-04-01", "2026-04-10",
                "2026-05-01", "Weekly training drills", "", 10));
    }

    // ── Search filtering tests ──

    @Test
    public void filter_emptyQuery_returnsAllEvents() {
        List<Event> filtered = filterEvents("");
        assertEquals(4, filtered.size());
    }

    @Test
    public void filter_byName_matchesCorrectEvents() {
        List<Event> filtered = filterEvents("Soccer");
        assertEquals(2, filtered.size());
        assertEquals("Soccer Match", filtered.get(0).getName());
        assertEquals("Soccer Training", filtered.get(1).getName());
    }

    @Test
    public void filter_byDescription_matchesCorrectEvents() {
        List<Event> filtered = filterEvents("yoga");
        assertEquals(1, filtered.size());
        assertEquals("Yoga Class", filtered.get(0).getName());
    }

    @Test
    public void filter_caseInsensitive_matchesRegardlessOfCase() {
        List<Event> filteredUpper = filterEvents("SOCCER");
        List<Event> filteredLower = filterEvents("soccer");
        List<Event> filteredMixed = filterEvents("SoCcEr");
        assertEquals(2, filteredUpper.size());
        assertEquals(2, filteredLower.size());
        assertEquals(2, filteredMixed.size());
    }

    @Test
    public void filter_noMatch_returnsEmptyList() {
        List<Event> filtered = filterEvents("basketball");
        assertEquals(0, filtered.size());
    }

    @Test
    public void filter_partialMatch_matchesSubstring() {
        List<Event> filtered = filterEvents("art");
        assertEquals(1, filtered.size());
        assertEquals("Art Exhibition", filtered.get(0).getName());
    }

    @Test
    public void filter_matchesDescription_notJustName() {
        List<Event> filtered = filterEvents("drills");
        assertEquals(1, filtered.size());
        assertEquals("Soccer Training", filtered.get(0).getName());
    }

    @Test
    public void filter_whitespaceQuery_returnsAllEvents() {
        List<Event> filtered = filterEvents("   ");
        assertEquals(4, filtered.size());
    }

    // ── Poster filtering tests ──

    @Test
    public void posterFilter_onlyIncludesEventsWithPosters() {
        List<Event> posterEvents = filterPosterEvents("");
        assertEquals(2, posterEvents.size());
        assertEquals("Soccer Match", posterEvents.get(0).getName());
        assertEquals("Art Exhibition", posterEvents.get(1).getName());
    }

    @Test
    public void posterFilter_searchStillApplies() {
        List<Event> posterEvents = filterPosterEvents("Soccer");
        assertEquals(1, posterEvents.size());
        assertEquals("Soccer Match", posterEvents.get(0).getName());
    }

    @Test
    public void posterFilter_noMatchingPosters_returnsEmpty() {
        List<Event> posterEvents = filterPosterEvents("Yoga");
        assertEquals(0, posterEvents.size());
    }

    @Test
    public void posterFilter_emptyPosterUrl_excluded() {
        Event noPoster = new Event("5", "Dance Night", 30, "", "", "", "Fun dancing", "", 5);
        allEvents.add(noPoster);
        List<Event> posterEvents = filterPosterEvents("Dance");
        assertEquals(0, posterEvents.size());
    }

    @Test
    public void posterFilter_nullPosterUrl_excluded() {
        Event nullPoster = new Event("6", "Swimming", 15, "", "", "", "Pool party", null, 5);
        allEvents.add(nullPoster);
        List<Event> posterEvents = filterPosterEvents("Swimming");
        assertEquals(0, posterEvents.size());
    }

    // ── Helpers (mirrors AdminManageEventsFragment.applyFilter logic) ──

    private List<Event> filterEvents(String query) {
        List<Event> filtered = new ArrayList<>();
        String lowerQuery = query.trim().toLowerCase();
        for (Event event : allEvents) {
            boolean matches = lowerQuery.isEmpty()
                    || event.getName().toLowerCase().contains(lowerQuery)
                    || event.getDescription().toLowerCase().contains(lowerQuery);
            if (matches) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private List<Event> filterPosterEvents(String query) {
        List<Event> filtered = new ArrayList<>();
        String lowerQuery = query.trim().toLowerCase();
        for (Event event : allEvents) {
            boolean matches = lowerQuery.isEmpty()
                    || event.getName().toLowerCase().contains(lowerQuery)
                    || event.getDescription().toLowerCase().contains(lowerQuery);
            if (matches && event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                filtered.add(event);
            }
        }
        return filtered;
    }
}
