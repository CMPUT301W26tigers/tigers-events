package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for enrolled entrant behaviour, now backed by the unified {@link Entrant} model.
 * EnrolledEntrant was removed as it was a redundant duplicate of Entrant.
 */
public class EnrolledEntrantsTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        Entrant entrant = new Entrant(
                "asevern",
                "event123",
                "Asher Severn",
                "asher@gmail.com",
                Entrant.Status.ACCEPTED
        );
        entrant.setUserId("asevern");

        assertEquals("asevern", entrant.getUserId());
        assertEquals("Asher Severn", entrant.getName());
        assertEquals("asher@gmail.com", entrant.getEmail());
        assertEquals(Entrant.Status.ACCEPTED, entrant.getStatus());
    }

    @Test
    public void emptyConstructor_createsObject() {
        Entrant entrant = new Entrant();
        assertNotNull(entrant);
    }

    @Test
    public void setUserId_updatesUserId() {
        Entrant entrant = new Entrant();
        entrant.setUserId("asevern");
        assertEquals("asevern", entrant.getUserId());
    }

    @Test
    public void setName_updatesName() {
        Entrant entrant = new Entrant();
        entrant.setName("Asher Severn");
        assertEquals("Asher Severn", entrant.getName());
    }

    @Test
    public void setEmail_updatesEmail() {
        Entrant entrant = new Entrant();
        entrant.setEmail("asher@gmail.com");
        assertEquals("asher@gmail.com", entrant.getEmail());
    }

    @Test
    public void setStatus_updatesStatus() {
        Entrant entrant = new Entrant();
        entrant.setStatus(Entrant.Status.ACCEPTED);
        assertEquals(Entrant.Status.ACCEPTED, entrant.getStatus());
    }
}
