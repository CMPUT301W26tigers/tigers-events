package com.example.eventsapp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the Lottery Algorithm.
 */
public class LotteryAlgorithmTest {

    private LotteryNotificationController controller;
    private final String eventId = "edge_case_event_123";

    @Before
    public void setUp() {
        controller = new LotteryNotificationController();
    }

    /**
     * Edge Case: The available spots are 0.
     * Expected: The lottery should run and select no one.
     */
    @Test
    public void testDrawWithZeroCapacity() {
        List<Entrant> entrants = generateEntrants(5, Entrant.Status.APPLIED);
        List<Entrant> selected = controller.performSelection(entrants, 0, 0);

        assertNotNull(selected);
        assertEquals("Should select 0 entrants when capacity is 0", 0, selected.size());
    }

    /**
     * Edge Case: The event capacity is perfectly full (occupied == capacity).
     * Expected: No new entrants are selected.
     */
    @Test
    public void testAllSpotsOccupied() {
        List<Entrant> entrants = generateEntrants(10, Entrant.Status.APPLIED);
        // Capacity is 5, but 5 spots are already occupied
        List<Entrant> selected = controller.performSelection(entrants, 5, 5);

        assertEquals("Should select 0 entrants when all spots are occupied", 0, selected.size());
    }

    /**
     * Edge Case: Occupied spots EXCEED the capacity (e.g., Organizer reduced capacity AFTER inviting people).
     * Expected: Selects 0 entrants without crashing or throwing bounds exceptions.
     */
    @Test
    public void testOccupiedExceedsCapacity() {
        List<Entrant> entrants = generateEntrants(5, Entrant.Status.APPLIED);
        // Capacity is 5, but 10 spots are already occupied
        List<Entrant> selected = controller.performSelection(entrants, 5, 10);

        assertEquals("Should return empty list when occupied spots exceed capacity", 0, selected.size());
    }

    /**
     * Edge Case: The capacity is massively larger than the pool of available entrants.
     * Expected: It should safely select all available applicants and stop.
     */
    @Test
    public void testCapacityMassivelyExceedsPool() {
        List<Entrant> entrants = generateEntrants(3, Entrant.Status.APPLIED);
        // Drawing 1000 entrants from a pool of 3
        List<Entrant> selected = controller.performSelection(entrants, 1000, 0);

        assertEquals("Should select exactly the size of the applicant pool", 3, selected.size());
    }

    /**
     * Edge Case: The total capacity minus occupied matches exactly the applicant pool size.
     * Expected: All available applicants are drawn.
     */
    @Test
    public void testExactCapacityMatchesPool() {
        List<Entrant> entrants = generateEntrants(7, Entrant.Status.APPLIED);
        // Capacity 10, Occupied 3 -> Available = 7. Pool = 7.
        List<Entrant> selected = controller.performSelection(entrants, 10, 3);

        assertEquals("Should select all 7 available entrants", 7, selected.size());
    }

    /**
     * Edge Case: Ensure the lottery completely ignores applicants that do not have an "APPLIED" status.
     * Expected: Only "APPLIED" entrants are placed in the selection pool.
     */
    @Test
    public void testStatusFilteringIgnoresInvalidStates() {
        List<Entrant> entrants = new ArrayList<>();
        entrants.add(new Entrant("1", eventId, "Applied User", "1@t.com", Entrant.Status.APPLIED));
        entrants.add(new Entrant("2", eventId, "Invited User", "2@t.com", Entrant.Status.INVITED));
        entrants.add(new Entrant("3", eventId, "Accepted User", "3@t.com", Entrant.Status.ACCEPTED));
        entrants.add(new Entrant("4", eventId, "Declined User", "4@t.com", Entrant.Status.DECLINED));
        entrants.add(new Entrant("5", eventId, "Cancelled User", "5@t.com", Entrant.Status.CANCELLED));
        entrants.add(new Entrant("6", eventId, "Private User", "6@t.com", Entrant.Status.PRIVATE_INVITED));

        List<Entrant> selected = controller.performSelection(entrants, 5, 0);

        assertEquals("Only the 1 entrant with 'APPLIED' status should be eligible for the draw", 1, selected.size());
        assertEquals("Applied User", selected.get(0).getName());
    }

    /**
     * Edge Case: Negative numbers are passed for capacity or occupied spots.
     * Expected: No negative array exceptions.
     */
    @Test
    public void testNegativeCapacityAndOccupied() {
        List<Entrant> entrants = generateEntrants(5, Entrant.Status.APPLIED);
        List<Entrant> selected = controller.performSelection(entrants, -5, -2);

        assertNotNull(selected);
        assertEquals("Negative capacity should safely return 0 selections", 0, selected.size());
    }

    /**
     * Edge Case: An entirely empty list of entrants is passed.
     * Expected: Return an empty list without a NullPointerException.
     */
    @Test
    public void testEmptyEntrantPool() {
        List<Entrant> entrants = new ArrayList<>();
        List<Entrant> selected = controller.performSelection(entrants, 5, 0);

        assertNotNull(selected);
        assertEquals(0, selected.size());
    }

    /**
     * Test: A standard partial draw where random selection must happen.
     */
    @Test
    public void testStandardPartialDraw() {
        List<Entrant> entrants = generateEntrants(50, Entrant.Status.APPLIED);

        // Target 20 spots, but 5 are already occupied (meaning 15 spots left)
        List<Entrant> selected = controller.performSelection(entrants, 20, 5);

        assertEquals("Should select exactly 15 entrants", 15, selected.size());
        for (Entrant e : selected) {
            assertTrue("Selected entrant must be from the pool", entrants.contains(e));
        }
    }

    /**
     * Generates a list of dummy entrants for testing.
     */
    private List<Entrant> generateEntrants(int count, Entrant.Status status) {
        List<Entrant> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Entrant(
                    "user_" + i,
                    eventId,
                    "Test User " + i,
                    "user" + i + "@test.com",
                    status
            ));
        }
        return list;
    }
}