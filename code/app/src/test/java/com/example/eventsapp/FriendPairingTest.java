package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Unit tests for the lottery selection process, specifically focusing on "friend pairing" (group selection).
 * These tests ensure that groups are treated as a single entity during the draw but strictly respect headcount capacity.
 */
public class FriendPairingTest {

    private LotteryNotificationController controller;
    private final String eventId = "test_event_id";

    @Before
    public void setUp() {
        controller = new LotteryNotificationController();
    }

    @Test
    public void testIndividualSelection() {
        List<Entrant> entrants = new ArrayList<>();
        entrants.add(new Entrant("1", eventId, "User 1", "u1@test.com", Entrant.Status.APPLIED));
        entrants.add(new Entrant("2", eventId, "User 2", "u2@test.com", Entrant.Status.APPLIED));
        entrants.add(new Entrant("3", eventId, "User 3", "u3@test.com", Entrant.Status.APPLIED));

        // Capacity for 2 people
        List<Entrant> selected = controller.performSelection(entrants, 2, 0);

        assertEquals(2, selected.size());
    }

    @Test
    public void testGroupSelectionAllOrNone() {
        List<Entrant> entrants = new ArrayList<>();
        
        // Group 1: 3 people
        String groupId = "group_123";
        for (int i = 1; i <= 3; i++) {
            Entrant e = new Entrant("g" + i, eventId, "Group User " + i, "g" + i + "@test.com", Entrant.Status.APPLIED);
            e.setGroupId(groupId);
            entrants.add(e);
        }

        // Capacity for 3 people. Group fits.
        List<Entrant> selected = controller.performSelection(entrants, 3, 0);

        assertEquals(3, selected.size());
        for (Entrant e : selected) {
            assertEquals(groupId, e.getGroupId());
        }
    }

    @Test
    public void testGroupOverflowSkip() {
        List<Entrant> entrants = new ArrayList<>();
        
        // Group A: 3 people
        String groupAId = "groupA";
        for (int i = 1; i <= 3; i++) {
            Entrant e = new Entrant("ga" + i, eventId, "A" + i, "a" + i + "@t.com", Entrant.Status.APPLIED);
            e.setGroupId(groupAId);
            entrants.add(e);
        }

        // Individual B
        Entrant eb = new Entrant("b1", eventId, "B", "b@t.com", Entrant.Status.APPLIED);
        entrants.add(eb);

        // Capacity is 2 people. 
        // Group A (3 people) does not fit and must be skipped.
        // Individual B (1 person) fits and should be selected.
        List<Entrant> selected = controller.performSelection(entrants, 2, 0);

        assertEquals(1, selected.size());
        assertTrue(selected.contains(eb));
        for (Entrant e : selected) {
            assertFalse("Group A should have been skipped as it exceeds capacity", groupAId.equals(e.getGroupId()));
        }
    }

    @Test
    public void testMixedLotteryRespectsHeadcount() {
        List<Entrant> entrants = new ArrayList<>();
        
        // Capacity: 5 people
        // Entities: 
        // 1. Group G1 (3 people)
        // 2. Individual I1 (1 person)
        // 3. Individual I2 (1 person)
        // 4. Individual I3 (1 person)
        
        for (int i = 1; i <= 3; i++) {
            Entrant e = new Entrant("g1u" + i, eventId, "G1U" + i, "g1u" + i + "@t.com", Entrant.Status.APPLIED);
            e.setGroupId("G1");
            entrants.add(e);
        }
        
        Entrant i1 = new Entrant("i1", eventId, "I1", "i1@t.com", Entrant.Status.APPLIED);
        Entrant i2 = new Entrant("i2", eventId, "I2", "i2@t.com", Entrant.Status.APPLIED);
        Entrant i3 = new Entrant("i3", eventId, "I3", "i3@t.com", Entrant.Status.APPLIED);
        entrants.add(i1);
        entrants.add(i2);
        entrants.add(i3);

        List<Entrant> selected = controller.performSelection(entrants, 5, 0);

        // Total selected headcount should be at most 5.
        // Since candidates are G1(3), I1(1), I2(1), I3(1) -> total headcount available is 6.
        // Depending on shuffle, either (G1, I1, I2) or (I1, I2, I3, G1 - G1 skips) etc.
        assertTrue("Headcount should not exceed 5", selected.size() <= 5);
        
        // If G1 is selected, headcount is at least 3.
        // If G1 is not selected, headcount is 3 (I1, I2, I3).
        assertTrue("Headcount should be at least 3 in this setup", selected.size() >= 3);
    }

    @Test
    public void testCapacitySaturation() {
        List<Entrant> entrants = new ArrayList<>();
        entrants.add(new Entrant("1", eventId, "U1", "u1@t.com", Entrant.Status.APPLIED));
        
        // Draw 5 spots but only 1 person applied
        List<Entrant> selected = controller.performSelection(entrants, 5, 0);
        
        assertEquals(1, selected.size());
    }
}
