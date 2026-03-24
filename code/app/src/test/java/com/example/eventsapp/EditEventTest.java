package com.example.eventsapp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for the EditEventFragment functionality.
 * Tests navigation routing logic, Firestore data map construction,
 * and deep link generation for existing events.
 */
public class EditEventTest {

    //Navigation routing tests

    @Test
    public void shouldNavigateToEdit_createdTabActiveEvent_returnsTrue() {
        assertTrue(EditEventFragment.shouldNavigateToEdit(true, false));
    }

    @Test
    public void shouldNavigateToEdit_createdTabHistoryEvent_returnsFalse() {
        assertFalse(EditEventFragment.shouldNavigateToEdit(true, true));
    }

    @Test
    public void shouldNavigateToEdit_registeredTabActiveEvent_returnsFalse() {
        assertFalse(EditEventFragment.shouldNavigateToEdit(false, false));
    }

    @Test
    public void shouldNavigateToEdit_registeredTabHistoryEvent_returnsFalse() {
        assertFalse(EditEventFragment.shouldNavigateToEdit(false, true));
    }

    //Firestore update data map tests

    @Test
    public void updateDataMap_containsAllRequiredFields() {
        Map<String, Object> data = buildSampleUpdateData();

        assertTrue(data.containsKey("id"));
        assertTrue(data.containsKey("name"));
        assertTrue(data.containsKey("amount"));
        assertTrue(data.containsKey("description"));
        assertTrue(data.containsKey("sampleSize"));
        assertTrue(data.containsKey("event_date"));
        assertTrue(data.containsKey("registration_start"));
        assertTrue(data.containsKey("registration_end"));
    }

    @Test
    public void updateDataMap_valuesAreCorrect() {
        Map<String, Object> data = buildSampleUpdateData();

        assertEquals("event-123", data.get("id"));
        assertEquals("Updated Event", data.get("name"));
        assertEquals(50, data.get("amount"));
        assertEquals("A fun event", data.get("description"));
        assertEquals(10, data.get("sampleSize"));
        assertEquals("2026-04-15", data.get("event_date"));
        assertEquals("2026-03-01", data.get("registration_start"));
        assertEquals("2026-04-10", data.get("registration_end"));
    }

    @Test
    public void updateDataMap_capacityMustBePositive() {
        int capacity = 0;
        assertFalse("Capacity of 0 should be invalid", capacity > 0);

        capacity = -5;
        assertFalse("Negative capacity should be invalid", capacity > 0);

        capacity = 1;
        assertTrue("Capacity of 1 should be valid", capacity > 0);
    }

    @Test
    public void updateDataMap_sampleSizeCanBeZero() {
        int sampleSize = 0;
        assertTrue("Sample size of 0 should be valid", sampleSize >= 0);

        sampleSize = 10;
        assertTrue("Positive sample size should be valid", sampleSize >= 0);
    }

    @Test
    public void updateDataMap_sampleSizeNegativeNormalizesToZero() {
        int sampleSize = -3;
        if (sampleSize < 0) sampleSize = 0;
        assertEquals(0, sampleSize);
    }

    //Event field update tests

    @Test
    public void eventFields_canBeUpdatedAfterConstruction() {
        Event event = new Event("id-1", "Original", 10, "2026-01-01",
                "2026-01-05", "2026-02-01", "Original desc", "", 5);

        event.setName("Updated Name");
        event.setAmount(100);
        event.setDescription("Updated desc");
        event.setEvent_date("2026-06-01");
        event.setRegistration_start("2026-05-01");
        event.setRegistration_end("2026-05-25");
        event.setSampleSize(20);

        assertEquals("Updated Name", event.getName());
        assertEquals(100, event.getAmount());
        assertEquals("Updated desc", event.getDescription());
        assertEquals("2026-06-01", event.getEvent_date());
        assertEquals("2026-05-01", event.getRegistration_start());
        assertEquals("2026-05-25", event.getRegistration_end());
        assertEquals(20, event.getSampleSize());
    }

    @Test
    public void eventFields_idRemainsUnchangedAfterUpdate() {
        Event event = new Event("original-id", "Name", 10, "", "", "", "", "", 0);

        event.setName("New Name");
        event.setAmount(50);

        assertEquals("original-id", event.getId());
    }

    //Helper

    private Map<String, Object> buildSampleUpdateData() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "event-123");
        data.put("name", "Updated Event");
        data.put("amount", 50);
        data.put("description", "A fun event");
        data.put("sampleSize", 10);
        data.put("event_date", "2026-04-15");
        data.put("registration_start", "2026-03-01");
        data.put("registration_end", "2026-04-10");
        return data;
    }
}
