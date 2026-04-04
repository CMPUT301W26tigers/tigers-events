package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the Event.geolocationRequired organizer toggle.
 * Part of the geolocation feature (US 03.01.01).
 */
public class EventGeolocationFlagTest {

    private Event makeEvent() {
        return new Event("Test Event", 50);
    }

    @Test
    public void geolocationRequired_defaultsFalse() {
        assertFalse("Geolocation should be opt-in; default must be false",
                makeEvent().isGeolocationRequired());
    }

    @Test
    public void setGeolocationRequired_trueEnablesFlag() {
        Event ev = makeEvent();
        ev.setGeolocationRequired(true);
        assertTrue(ev.isGeolocationRequired());
    }

    @Test
    public void setGeolocationRequired_falseDisablesFlag() {
        Event ev = makeEvent();
        ev.setGeolocationRequired(true);
        ev.setGeolocationRequired(false);
        assertFalse("Flag should be toggleable back to false", ev.isGeolocationRequired());
    }

    @Test
    public void geolocationRequired_independentBetweenInstances() {
        Event required = makeEvent();
        Event notRequired = makeEvent();
        required.setGeolocationRequired(true);
        assertFalse("Setting flag on one Event must not affect another",
                notRequired.isGeolocationRequired());
    }
}
