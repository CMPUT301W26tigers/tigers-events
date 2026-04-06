package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Intent tests for Event model behavior used by the organizer and entrant flows.
 */
public class EventModelTest {

    @Test
    public void getFormattedEventDate_formatsIsoDateForDisplay() {
        Event event = new Event("Northern Lights Networking Night", 80);
        event.setEvent_date("2026-11-05");

        assertEquals("November 5, 2026", event.getFormattedEventDate());
    }

    @Test
    public void getFormattedEventDate_returnsNoDateWhenUnset() {
        Event event = new Event("Faculty Mixer", 40);

        assertEquals("No date", event.getFormattedEventDate());
    }

    @Test
    public void getFormattedEventDate_fallsBackToRawValueWhenInputIsInvalid() {
        Event event = new Event("Winter Startup Pitch", 25);
        event.setEvent_date("2026-02-30");

        assertEquals("2026-02-30", event.getFormattedEventDate());
    }

    @Test
    public void getEventDeepLink_usesEventIdInQrTarget() {
        Event event = new Event("evt-ua-2026-001", "Campus Innovation Expo", 120,
                "2026-04-01", "2026-04-12", "2026-04-20",
                "A showcase for student projects.", "", 20);

        assertEquals("tigers-events://event/evt-ua-2026-001", event.getEventDeepLink());
    }

    @Test
    public void waitlistCapacity_negativeInput_clampsToUnlimitedSentinel() {
        Event event = new Event("River Valley Sunset Run", 150);

        event.setWaitlistCapacity(-10);

        assertEquals(0, event.getWaitlistCapacity());
    }

    @Test
    public void location_roundTripsRealVenueName() {
        Event event = new Event("Alumni Fireside Chat", 60);

        event.setLocation("University of Alberta Butterdome, Edmonton");

        assertEquals("University of Alberta Butterdome, Edmonton", event.getLocation());
    }

    @Test
    public void historyFlags_doNotAffectLiveEventDefaults() {
        Event liveEvent = new Event("Spring Research Symposium", 200);

        assertFalse(liveEvent.isFromHistory());
        assertEquals(null, liveEvent.getEntrantStatus());
        assertTrue(liveEvent.getId() != null && !liveEvent.getId().isEmpty());
    }
}
