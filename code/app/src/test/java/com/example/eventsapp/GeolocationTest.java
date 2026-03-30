package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GeolocationTest {

    @Test
    public void entrant_hasLocation_falseWhenUnset() {
        Entrant e = new Entrant("1", "ev", "n", "mail", Entrant.Status.APPLIED);
        assertFalse(e.hasLocation());
    }

    @Test
    public void entrant_hasLocation_trueForValidCoords() {
        Entrant e = new Entrant("1", "ev", "n", "mail", Entrant.Status.APPLIED);
        e.setLatitude(53.5);
        e.setLongitude(-113.5);
        assertTrue(e.hasLocation());
    }

    @Test
    public void entrant_hasLocation_falseAtNullIsland() {
        Entrant e = new Entrant("1", "ev", "n", "mail", Entrant.Status.APPLIED);
        e.setLatitude(0.0);
        e.setLongitude(0.0);
        assertFalse(e.hasLocation());
    }

    @Test
    public void entrant_hasLocation_falseWhenOutOfRange() {
        Entrant e = new Entrant("1", "ev", "n", "mail", Entrant.Status.APPLIED);
        e.setLatitude(100);
        e.setLongitude(0);
        assertFalse(e.hasLocation());
    }

    @Test
    public void event_geolocationRequired_defaultsFalse() {
        Event ev = new Event("Party", 10);
        assertFalse(ev.isGeolocationRequired());
        ev.setGeolocationRequired(true);
        assertTrue(ev.isGeolocationRequired());
    }
}
