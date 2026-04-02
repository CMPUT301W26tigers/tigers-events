package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for user information management and updates.
 * These tests verify the logic used by AccountFragment to manage user data.
 */
public class AccountFragmentTest {

    private Users testUser;

    @Before
    public void setUp() {
        // Initialize a test user with mock data
        testUser = new Users("John Doe", "john@example.com", "password123", "1234567890");
        testUser.setId("test_user_id");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        // Set the current user in UserManager
        UserManager.getInstance().setCurrentUser(testUser);
    }

    @Test
    public void testUserManager_SetAndGetCurrentUser() {
        assertEquals(testUser, UserManager.getInstance().getCurrentUser());
    }

    @Test
    public void testUserUpdateName() {
        // Simulate updating name field
        String newName = "Jane Smith";
        testUser.setName(newName);
        assertEquals(newName, UserManager.getInstance().getCurrentUser().getName());
    }

    @Test
    public void testUserUpdateEmail() {
        // Simulate updating email field
        String newEmail = "jane.smith@example.com";
        testUser.setEmail(newEmail);
        assertEquals(newEmail, UserManager.getInstance().getCurrentUser().getEmail());
    }

    @Test
    public void testGreetingLogic() {
        // AccountFragment uses firstName for greeting if available, otherwise name
        
        // Scenario 1: Both firstName and name available
        testUser.setFirstName("Johnny");
        testUser.setName("Johnny Test");
        String greetingName = (testUser.getFirstName() != null && !testUser.getFirstName().isEmpty()) 
                ? testUser.getFirstName() : testUser.getName();
        assertEquals("Johnny", greetingName);

        // Scenario 2: Only name available (firstName empty)
        testUser.setFirstName("");
        testUser.setName("Johnny Test");
        greetingName = (testUser.getFirstName() != null && !testUser.getFirstName().isEmpty()) 
                ? testUser.getFirstName() : testUser.getName();
        assertEquals("Johnny Test", greetingName);

        // Scenario 3: firstName is null
        testUser.setFirstName(null);
        greetingName = (testUser.getFirstName() != null && !testUser.getFirstName().isEmpty()) 
                ? testUser.getFirstName() : testUser.getName();
        assertEquals("Johnny Test", greetingName);
    }

    @Test
    public void testUserUpdatePhoneNumber() {
        // Test with String
        testUser.setPhoneNumber("0987654321");
        assertEquals("0987654321", UserManager.getInstance().getCurrentUser().getPhoneNumber());

        // Test with Long (simulating Firestore behavior where numbers are often returned as Long)
        testUser.setPhoneNumber(1234567890L);
        assertEquals("1234567890", UserManager.getInstance().getCurrentUser().getPhoneNumber());

        // Test with null
        testUser.setPhoneNumber(null);
        assertNull(UserManager.getInstance().getCurrentUser().getPhoneNumber());
    }

    @Test
    public void testUserUpdateLocation() {
        String newLocation = "Edmonton, AB";
        testUser.setLocation(newLocation);
        assertEquals(newLocation, UserManager.getInstance().getCurrentUser().getLocation());
    }

    @Test
    public void testUserUpdateAccountType() {
        String newType = "Admin";
        testUser.setAccountType(newType);
        assertEquals(newType, UserManager.getInstance().getCurrentUser().getAccountType());
    }

    @Test
    public void testUserUpdateNotificationsEnabled() {
        testUser.setNotificationsEnabled(true);
        assertTrue(UserManager.getInstance().getCurrentUser().isNotificationsEnabled());
        
        testUser.setNotificationsEnabled(false);
        assertFalse(UserManager.getInstance().getCurrentUser().isNotificationsEnabled());
    }

    @Test
    public void testUserDeviceIdUpdate() {
        String newDeviceId = "new_test_device_789";
        testUser.setDeviceId(newDeviceId);
        assertEquals(newDeviceId, UserManager.getInstance().getCurrentUser().getDeviceId());
        
        // Simulate clearing device ID on sign out
        testUser.setDeviceId(null);
        assertNull(UserManager.getInstance().getCurrentUser().getDeviceId());
    }

    @Test
    public void testSignOutAndSessionClear() {
        // Ensure user is logged in
        assertTrue(UserManager.getInstance().isLoggedIn());

        // Perform logout
        UserManager.getInstance().logout();

        // Verify session is cleared
        assertNull(UserManager.getInstance().getCurrentUser());
        assertFalse(UserManager.getInstance().isLoggedIn());
    }

    @Test
    public void testDeleteAccountClearsSession() {
        // Ensure user is logged in
        assertTrue(UserManager.getInstance().isLoggedIn());
        assertEquals("test_user_id", UserManager.getInstance().getCurrentUser().getId());

        // Simulate what deleteAccount() does after Firestore deletion succeeds
        UserManager.getInstance().setCurrentUser(null);

        // Verify session is cleared
        assertNull(UserManager.getInstance().getCurrentUser());
        assertFalse(UserManager.getInstance().isLoggedIn());
    }
}
