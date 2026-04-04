package com.example.eventsapp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExploreFilterHelper}.
 * All tests run on the local JVM — no Android framework needed.
 */
public class ExploreFilterHelperTest {

    private Event makeEvent(String name, String regEnd, String eventDate) {
        Event e = new Event("id-1", name, 10, "2026-01-01", regEnd, eventDate, "desc", "", 5);
        return e;
    }

    // ───────────────────── Text Filter ─────────────────────

    @Test
    public void textFilter_emptyQuery_matchesAll() {
        Event e = makeEvent("Soccer Night", "2026-05-01", "2026-06-01");
        assertTrue(ExploreFilterHelper.matchesTextFilter(e, ""));
        assertTrue(ExploreFilterHelper.matchesTextFilter(e, null));
    }

    @Test
    public void textFilter_matchingName_returnsTrue() {
        Event e = makeEvent("Soccer Night", "2026-05-01", "2026-06-01");
        assertTrue(ExploreFilterHelper.matchesTextFilter(e, "Soccer"));
        assertTrue(ExploreFilterHelper.matchesTextFilter(e, "Night"));
        assertTrue(ExploreFilterHelper.matchesTextFilter(e, "cer Ni"));
    }

    @Test
    public void textFilter_nonMatchingName_returnsFalse() {
        Event e = makeEvent("Soccer Night", "2026-05-01", "2026-06-01");
        assertFalse(ExploreFilterHelper.matchesTextFilter(e, "Basketball"));
    }

    @Test
    public void textFilter_caseInsensitive() {
        Event e = makeEvent("Skate Night", "2026-05-01", "2026-06-01");
        assertTrue(ExploreFilterHelper.matchesTextFilter(e, "SKATE"));
        assertTrue(ExploreFilterHelper.matchesTextFilter(e, "skate night"));
    }

    // ───────────────────── Available Only ─────────────────────

    @Test
    public void availableOnly_false_alwaysMatches() {
        Event e = makeEvent("Test", "2020-01-01", "2020-02-01");
        assertTrue(ExploreFilterHelper.matchesAvailableOnly(e, false, "2026-04-01"));
    }

    @Test
    public void availableOnly_registrationOpen_matches() {
        Event e = makeEvent("Test", "2026-12-31", "2027-01-15");
        assertTrue(ExploreFilterHelper.matchesAvailableOnly(e, true, "2026-04-01"));
    }

    @Test
    public void availableOnly_registrationClosed_excluded() {
        Event e = makeEvent("Test", "2026-03-01", "2026-04-01");
        assertFalse(ExploreFilterHelper.matchesAvailableOnly(e, true, "2026-04-02"));
    }

    @Test
    public void availableOnly_registrationEndsToday_matches() {
        Event e = makeEvent("Test", "2026-04-02", "2026-04-15");
        assertTrue(ExploreFilterHelper.matchesAvailableOnly(e, true, "2026-04-02"));
    }

    @Test
    public void availableOnly_emptyRegEnd_excluded() {
        Event e = makeEvent("Test", "", "2026-06-01");
        assertFalse(ExploreFilterHelper.matchesAvailableOnly(e, true, "2026-04-01"));
    }

    // ───────────────────── Date Range ─────────────────────

    @Test
    public void dateRange_noBounds_matchesAll() {
        Event e = makeEvent("Test", "2026-05-01", "2026-06-01");
        assertTrue(ExploreFilterHelper.matchesDateRange(e, null, null));
        assertTrue(ExploreFilterHelper.matchesDateRange(e, "", ""));
    }

    @Test
    public void dateRange_onlyFrom_filtersCorrectly() {
        Event e1 = makeEvent("Test", "2026-05-01", "2026-06-01");
        Event e2 = makeEvent("Test", "2026-05-01", "2026-03-01");
        assertTrue(ExploreFilterHelper.matchesDateRange(e1, "2026-04-01", null));
        assertFalse(ExploreFilterHelper.matchesDateRange(e2, "2026-04-01", null));
    }

    @Test
    public void dateRange_onlyTo_filtersCorrectly() {
        Event e1 = makeEvent("Test", "2026-05-01", "2026-03-15");
        Event e2 = makeEvent("Test", "2026-05-01", "2026-06-01");
        assertTrue(ExploreFilterHelper.matchesDateRange(e1, null, "2026-04-01"));
        assertFalse(ExploreFilterHelper.matchesDateRange(e2, null, "2026-04-01"));
    }

    @Test
    public void dateRange_bothBounds_inclusive() {
        Event e = makeEvent("Test", "2026-05-01", "2026-04-01");
        assertTrue(ExploreFilterHelper.matchesDateRange(e, "2026-04-01", "2026-04-30"));

        Event e2 = makeEvent("Test", "2026-05-01", "2026-04-30");
        assertTrue(ExploreFilterHelper.matchesDateRange(e2, "2026-04-01", "2026-04-30"));
    }

    @Test
    public void dateRange_outsideRange_excluded() {
        Event e = makeEvent("Test", "2026-05-01", "2026-06-15");
        assertFalse(ExploreFilterHelper.matchesDateRange(e, "2026-04-01", "2026-04-30"));
    }

    @Test
    public void dateRange_emptyEventDate_excluded() {
        Event e = makeEvent("Test", "2026-05-01", "");
        assertFalse(ExploreFilterHelper.matchesDateRange(e, "2026-04-01", "2026-04-30"));
    }

    // ───────────────────── Combined Filters ─────────────────────

    @Test
    public void allFilters_noFilters_returnsAll() {
        Event e1 = makeEvent("Soccer", "2026-05-01", "2026-06-01");
        Event e2 = makeEvent("Basketball", "2026-03-01", "2026-04-01");
        List<Event> result = ExploreFilterHelper.applyAllFilters(
                Arrays.asList(e1, e2), "", false, null, null, "2026-04-02");
        assertEquals(2, result.size());
    }

    @Test
    public void allFilters_combined_onlyMatchingEventsPass() {
        Event soccer = makeEvent("Soccer Game", "2026-06-01", "2026-06-15");
        Event basketball = makeEvent("Basketball Game", "2026-06-01", "2026-06-20");
        Event pastSoccer = makeEvent("Soccer Clinic", "2026-03-01", "2026-03-15");
        Event futeSoccer = makeEvent("Soccer Camp", "2026-08-01", "2026-08-15");

        List<Event> all = Arrays.asList(soccer, basketball, pastSoccer, futeSoccer);

        // Text: "Soccer", Available: true (today=2026-04-02), Date range: June only
        List<Event> result = ExploreFilterHelper.applyAllFilters(
                all, "Soccer", true, "2026-06-01", "2026-06-30", "2026-04-02");

        assertEquals(1, result.size());
        assertEquals("Soccer Game", result.get(0).getName());
    }

    @Test
    public void allFilters_emptyList_returnsEmpty() {
        List<Event> result = ExploreFilterHelper.applyAllFilters(
                Collections.emptyList(), "test", true, "2026-01-01", "2026-12-31", "2026-04-02");
        assertTrue(result.isEmpty());
    }
}
