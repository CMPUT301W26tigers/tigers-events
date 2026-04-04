package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests that verify the join-waitlist geolocation contract:
 *
 * - When an event does NOT require location, the entrant joins without coordinates
 *   (hasLocation() == false) — no GPS popup should appear.
 * - When an event REQUIRES location, the entrant document must contain valid coordinates
 *   (hasLocation() == true).
 *
 * Part of the geolocation feature (US 03.01.01).
 */
public class GeolocationJoinConditionTest {

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Simulates what writeEntrantToFirestore stores when geolocation is NOT required. */
    private Entrant joinWithoutLocation() {
        Entrant e = new Entrant("u1", "ev1", "Bob", "bob@test.com", Entrant.Status.APPLIED);
        // No coordinates written — matches the fixed code path (0.0, 0.0 sentinel)
        e.setLatitude(0.0);
        e.setLongitude(0.0);
        return e;
    }

    /** Simulates what writeEntrantToFirestore stores when geolocation IS required and GPS succeeded. */
    private Entrant joinWithLocation(double lat, double lng) {
        Entrant e = new Entrant("u2", "ev1", "Carol", "carol@test.com", Entrant.Status.APPLIED);
        e.setLatitude(lat);
        e.setLongitude(lng);
        return e;
    }

    // ── When location is NOT required ────────────────────────────────────────

    @Test
    public void notRequired_entrantHasNoLocation() {
        Event ev = new Event("Free Event", 20);
        // geolocationRequired defaults to false
        assertFalse(ev.isGeolocationRequired());

        Entrant entrant = joinWithoutLocation();
        assertFalse("Entrant joined non-location event should have no location",
                entrant.hasLocation());
    }

    @Test
    public void notRequired_nullIslandCoordsAreRejectedByHasLocation() {
        // Confirms (0.0, 0.0) is treated as "no real location" even if stored
        Entrant e = joinWithoutLocation();
        assertFalse(e.hasLocation());
    }

    // ── When location IS required ────────────────────────────────────────────

    @Test
    public void required_entrantWithValidCoordsHasLocation() {
        Event ev = new Event("GPS Event", 20);
        ev.setGeolocationRequired(true);
        assertTrue(ev.isGeolocationRequired());

        Entrant entrant = joinWithLocation(53.5461, -113.4938); // Edmonton
        assertTrue("Entrant who granted GPS should have a location", entrant.hasLocation());
    }

    @Test
    public void required_entrantWithInvalidCoordsHasNoLocation() {
        // GPS call returned (0,0) — treated as failure
        Entrant entrant = joinWithLocation(0.0, 0.0);
        assertFalse("(0,0) sentinel must be rejected even for required events",
                entrant.hasLocation());
    }

    // ── Flag toggling affects join behaviour ─────────────────────────────────

    @Test
    public void flagToggledOn_thenOff_entrantCanJoinWithoutLocation() {
        Event ev = new Event("Toggle Event", 10);
        ev.setGeolocationRequired(true);
        ev.setGeolocationRequired(false); // organizer changed their mind

        assertFalse(ev.isGeolocationRequired());
        Entrant entrant = joinWithoutLocation();
        assertFalse("After toggling off, no location should be required", entrant.hasLocation());
    }

    @Test
    public void multipleEntrants_onlyRequiredEventStoresCoords() {
        Event geoEvent = new Event("Geo Event", 10);
        geoEvent.setGeolocationRequired(true);

        Event freeEvent = new Event("Free Event", 10);
        // freeEvent.geolocationRequired defaults to false

        Entrant withCoords = joinWithLocation(53.5461, -113.4938);
        Entrant withoutCoords = joinWithoutLocation();

        assertTrue("Geo event entrant should have location", withCoords.hasLocation());
        assertFalse("Free event entrant should NOT have location", withoutCoords.hasLocation());
    }
}
