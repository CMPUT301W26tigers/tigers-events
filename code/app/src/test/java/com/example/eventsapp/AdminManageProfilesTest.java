package com.example.eventsapp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the search/filter logic used in AdminManageProfilesFragment.
 * The filtering algorithm is replicated here to validate it as a pure unit test.
 */
public class AdminManageProfilesTest {

    private List<Users> allUsers;

    @Before
    public void setUp() {
        allUsers = new ArrayList<>();

        Users user1 = new Users("Alice", "Smith", "alice@example.com", "Edmonton, AB", "Entrant", true);
        user1.setId("u1");
        allUsers.add(user1);

        Users user2 = new Users("Bob", "Jones", "bob@example.com", "Edmonton, AB", "Organizer", true);
        user2.setId("u2");
        allUsers.add(user2);

        Users user3 = new Users("Charlie", "Brown", "charlie@example.com", "Edmonton, AB", "Admin", true);
        user3.setId("u3");
        allUsers.add(user3);

        // User with only firstName/lastName, no name field set via name setter
        Users user4 = new Users();
        user4.setFirstName("Diana");
        user4.setLastName("Prince");
        user4.setEmail("diana@example.com");
        user4.setId("u4");
        allUsers.add(user4);
    }

    /**
     * Replicates the search filter logic from AdminManageProfilesFragment.
     */
    private List<Users> filterUsers(String query) {
        List<Users> filtered = new ArrayList<>();
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            filtered.addAll(allUsers);
            return filtered;
        }
        for (Users u : allUsers) {
            String name = u.getName();
            if (name == null || name.isEmpty()) {
                String first = u.getFirstName() != null ? u.getFirstName() : "";
                String last = u.getLastName() != null ? u.getLastName() : "";
                name = (first + " " + last).trim();
            }
            if (name.toLowerCase().contains(q)
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))) {
                filtered.add(u);
            }
        }
        return filtered;
    }

    // Search filter tests

    @Test
    public void searchFilter_emptyQuery_returnsAllUsers() {
        List<Users> result = filterUsers("");
        assertEquals(allUsers.size(), result.size());
    }

    @Test
    public void searchFilter_exactNameMatch_returnsOneUser() {
        List<Users> result = filterUsers("Alice Smith");
        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getId());
    }

    @Test
    public void searchFilter_partialNameMatch_returnsMatchingUsers() {
        List<Users> result = filterUsers("ali");
        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getId());
    }

    @Test
    public void searchFilter_emailMatch_returnsMatchingUser() {
        List<Users> result = filterUsers("bob@example");
        assertEquals(1, result.size());
        assertEquals("u2", result.get(0).getId());
    }

    @Test
    public void searchFilter_noMatch_returnsEmptyList() {
        List<Users> result = filterUsers("zzzzzzz");
        assertTrue(result.isEmpty());
    }

    @Test
    public void searchFilter_caseInsensitive_returnsMatch() {
        List<Users> result = filterUsers("ALICE");
        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getId());
    }

    @Test
    public void searchFilter_userWithOnlyFirstLastName_isSearchable() {
        List<Users> result = filterUsers("Diana");
        assertEquals(1, result.size());
        assertEquals("u4", result.get(0).getId());
    }

    @Test
    public void searchFilter_whitespaceOnlyQuery_returnsAllUsers() {
        List<Users> result = filterUsers("   ");
        assertEquals(allUsers.size(), result.size());
    }

    @Test
    public void searchFilter_multipleMatches_returnsAll() {
        // "example.com" appears in all users' emails
        List<Users> result = filterUsers("example.com");
        assertEquals(allUsers.size(), result.size());
    }
}
