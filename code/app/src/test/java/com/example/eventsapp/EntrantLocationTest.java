package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Entrant latitude / longitude storage and hasLocation().
 * Part of the geolocation feature (US 03.01.01).
 */
public class EntrantLocationTest {

    private Entrant makeEntrant() {
        return new Entrant("u1", "ev1", "Alice", "alice@example.com", Entrant.Status.APPLIED);
    }

    // ── hasLocation: false cases ─────────────────────────────────────────────

    @Test
    public void hasLocation_falseByDefault() {
        assertFalse("New entrant should have no location", makeEntrant().hasLocation());
    }

    @Test
    public void hasLocation_falseAtNullIsland() {
        Entrant e = makeEntrant();
        e.setLatitude(0.0);
        e.setLongitude(0.0);
        assertFalse("(0,0) is the Null Island sentinel — should be rejected", e.hasLocation());
    }

    @Test
    public void hasLocation_falseWhenLatitudeOutOfRange() {
        Entrant e = makeEntrant();
        e.setLatitude(91.0);
        e.setLongitude(0.0);
        assertFalse("Latitude > 90 is invalid", e.hasLocation());
    }

    @Test
    public void hasLocation_falseWhenLongitudeOutOfRange() {
        Entrant e = makeEntrant();
        e.setLatitude(0.0);
        e.setLongitude(181.0);
        assertFalse("Longitude > 180 is invalid", e.hasLocation());
    }

    @Test
    public void hasLocation_falseWhenNegativeLatitudeOutOfRange() {
        Entrant e = makeEntrant();
        e.setLatitude(-91.0);
        e.setLongitude(-113.0);
        assertFalse("Latitude < -90 is invalid", e.hasLocation());
    }

    // ── hasLocation: true cases ──────────────────────────────────────────────

    @Test
    public void hasLocation_trueForEdmontonCoords() {
        Entrant e = makeEntrant();
        e.setLatitude(53.5461);
        e.setLongitude(-113.4938);
        assertTrue("Edmonton coordinates should be valid", e.hasLocation());
    }

    @Test
    public void hasLocation_trueForSydneyCoords() {
        Entrant e = makeEntrant();
        e.setLatitude(-33.8688);
        e.setLongitude(151.2093);
        assertTrue("Sydney coordinates (southern hemisphere) should be valid", e.hasLocation());
    }

    @Test
    public void hasLocation_trueAtBoundaryValues() {
        Entrant e = makeEntrant();
        e.setLatitude(90.0);
        e.setLongitude(180.0);
        assertTrue("Exact boundary ±90/±180 should be valid", e.hasLocation());
    }

    // ── getter / setter round-trip ───────────────────────────────────────────

    @Test
    public void setLatitude_persistsValue() {
        Entrant e = makeEntrant();
        e.setLatitude(53.5461);
        assertEquals(53.5461, e.getLatitude(), 1e-9);
    }

    @Test
    public void setLongitude_persistsValue() {
        Entrant e = makeEntrant();
        e.setLongitude(-113.4938);
        assertEquals(-113.4938, e.getLongitude(), 1e-9);
    }
}
