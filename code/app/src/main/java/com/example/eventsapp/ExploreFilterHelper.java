package com.example.eventsapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure filter logic for the Explore page. No Android dependencies so it can be unit-tested on JVM.
 */
public class ExploreFilterHelper {

    /**
     * Returns true if the event name contains the query (case-insensitive).
     * An empty query matches everything.
     */
    public static boolean matchesTextFilter(Event event, String query) {
        if (query == null || query.isEmpty()) return true;
        return event.getName().toLowerCase().contains(query.toLowerCase());
    }

    /**
     * Returns true if the event's registration is still open (registration_end >= today).
     * When availableOnly is false, always returns true.
     */
    public static boolean matchesAvailableOnly(Event event, boolean availableOnly, String todayDateStr) {
        if (!availableOnly) return true;
        String regEnd = event.getRegistration_end();
        if (regEnd == null || regEnd.isEmpty()) return false;
        return regEnd.compareTo(todayDateStr) >= 0;
    }

    /**
     * Returns true if the event's date falls within [dateFrom, dateTo] inclusive.
     * Null/empty bounds are treated as unbounded.
     */
    public static boolean matchesDateRange(Event event, String dateFrom, String dateTo) {
        boolean hasFrom = dateFrom != null && !dateFrom.isEmpty();
        boolean hasTo = dateTo != null && !dateTo.isEmpty();
        if (!hasFrom && !hasTo) return true;

        String eventDate = event.getEvent_date();
        if (eventDate == null || eventDate.isEmpty()) return false;

        if (hasFrom && eventDate.compareTo(dateFrom) < 0) return false;
        if (hasTo && eventDate.compareTo(dateTo) > 0) return false;
        return true;
    }

    /**
     * Applies all filters (text, available-only, date range) with AND logic.
     * Returns a new list containing only the events that pass all active filters.
     */
    public static List<Event> applyAllFilters(List<Event> events, String query,
                                               boolean availableOnly, String dateFrom,
                                               String dateTo, String todayDateStr) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (matchesTextFilter(event, query)
                    && matchesAvailableOnly(event, availableOnly, todayDateStr)
                    && matchesDateRange(event, dateFrom, dateTo)) {
                result.add(event);
            }
        }
        return result;
    }
}
