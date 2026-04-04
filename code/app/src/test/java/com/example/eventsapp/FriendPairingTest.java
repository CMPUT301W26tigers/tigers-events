package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Unit tests for the lottery selection process, specifically focusing on "friend pairing" (group selection).
 * These tests ensure that groups are treated as a single entity during the draw but bring in all members if selected.
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

        // Draw 2 individuals
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

        // Capacity is 1 entity (meaning the group should be picked as one)
        List<Entrant> selected = controller.performSelection(entrants, 1, 0);

        // If the group is picked, all 3 members should be present
        assertEquals(3, selected.size());
        for (Entrant e : selected) {
            assertEquals(groupId, e.getGroupId());
        }
    }

    @Test
    public void testGroupCountsAsOneEntity() {
        List<Entrant> entrants = new ArrayList<>();
        
        // Group A: 2 people
        Entrant ga1 = new Entrant("ga1", eventId, "A1", "a1@t.com", Entrant.Status.APPLIED);
        ga1.setGroupId("A");
        Entrant ga2 = new Entrant("ga2", eventId, "A2", "a2@t.com", Entrant.Status.APPLIED);
        ga2.setGroupId("A");
        entrants.add(ga1);
        entrants.add(ga2);

        // Individual B
        Entrant eb = new Entrant("b1", eventId, "B", "b@t.com", Entrant.Status.APPLIED);
        entrants.add(eb);

        // Capacity is 2 entities. 
        // Candidate pool: {Group A, Individual B}. Both should be selected.
        List<Entrant> selected = controller.performSelection(entrants, 2, 0);

        // Total of 3 entrants selected (2 from A + 1 from B)
        assertEquals(3, selected.size());
        assertTrue(selected.contains(ga1));
        assertTrue(selected.contains(ga2));
        assertTrue(selected.contains(eb));
    }

    @Test
    public void testMixedLotteryIntegrity() {
        List<Entrant> entrants = new ArrayList<>();
        
        // 10 people total: 2 groups of 3, and 4 individuals
        // Candidate entities: 6 (2 groups + 4 individuals)
        
        for (int g = 1; g <= 2; g++) {
            String gid = "group_" + g;
            for (int i = 1; i <= 3; i++) {
                Entrant e = new Entrant("g" + g + "u" + i, eventId, "Group" + g + "U" + i, "g" + g + "u" + i + "@t.com", Entrant.Status.APPLIED);
                e.setGroupId(gid);
                entrants.add(e);
            }
        }
        
        for (int i = 1; i <= 4; i++) {
            entrants.add(new Entrant("indiv" + i, eventId, "Indiv" + i, "i" + i + "@t.com", Entrant.Status.APPLIED));
        }

        // Draw 3 entities
        List<Entrant> selected = controller.performSelection(entrants, 3, 0);

        // We verify that exactly 3 unique entities were chosen.
        Set<String> selectedGroupIds = new HashSet<>();
        int individualCount = 0;

        for (Entrant e : selected) {
            if (e.getGroupId() != null && !e.getGroupId().isEmpty()) {
                selectedGroupIds.add(e.getGroupId());
            } else {
                individualCount++;
            }
        }

        assertEquals("Should have selected exactly 3 entities", 3, selectedGroupIds.size() + individualCount);
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
