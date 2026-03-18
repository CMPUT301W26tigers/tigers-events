package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EnrolledEntrantsTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        EnrolledEntrant entrant = new EnrolledEntrant(
                "asevern",
                "Asher Severn",
                "asher@gmail.com",
                "Enrolled"
        );

        assertEquals("asevern", entrant.getUserId());
        assertEquals("Asher Severn", entrant.getName());
        assertEquals("asher@gmail.com", entrant.getEmail());
        assertEquals("Enrolled", entrant.getStatus());
    }

    @Test
    public void emptyConstructor_createsObject() {
        EnrolledEntrant entrant = new EnrolledEntrant();

        assertNotNull(entrant);
    }

    @Test
    public void setUserId_updatesUserId() {
        EnrolledEntrant entrant = new EnrolledEntrant();
        entrant.setUserId("asevern");

        assertEquals("asevern", entrant.getUserId());
    }

    @Test
    public void setName_updatesName() {
        EnrolledEntrant entrant = new EnrolledEntrant();
        entrant.setName("Asher Severn");

        assertEquals("Asher Severn", entrant.getName());
    }

    @Test
    public void setEmail_updatesEmail() {
        EnrolledEntrant entrant = new EnrolledEntrant();
        entrant.setEmail("asher@gmail.com");

        assertEquals("asher@gmail.com", entrant.getEmail());
    }

    @Test
    public void setStatus_updatesStatus() {
        EnrolledEntrant entrant = new EnrolledEntrant();
        entrant.setStatus("Accepted");

        assertEquals("Accepted", entrant.getStatus());
    }
}

