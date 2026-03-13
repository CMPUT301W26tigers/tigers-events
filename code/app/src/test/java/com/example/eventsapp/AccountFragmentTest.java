package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class AccountFragmentTest {

    private Users testUser;

    @Before
    public void setUp() {
        // Initialize a test user
        testUser = new Users("John Doe", "john@example.com", "password123", "1234567890");
        testUser.setId("test_user_id");
        // Set the current user in UserManager
        UserManager.getInstance().setCurrentUser(testUser);
    }

    @Test
    public void testUserManager_SetAndGetCurrentUser() {
        assertEquals(testUser, UserManager.getInstance().getCurrentUser());
    }

    @Test
    public void testUserUpdateName() {
        testUser.setName("Jane Doe");
        assertEquals("Jane Doe", UserManager.getInstance().getCurrentUser().getName());
    }

    @Test
    public void testUserUpdateEmail() {
        testUser.setEmail("jane@example.com");
        assertEquals("jane@example.com", UserManager.getInstance().getCurrentUser().getEmail());
    }

    @Test
    public void testUserDeviceId() {
        testUser.setDeviceId("device_id_123");
        assertEquals("device_id_123", UserManager.getInstance().getCurrentUser().getDeviceId());
        
        testUser.setDeviceId(null);
        assertNull(UserManager.getInstance().getCurrentUser().getDeviceId());
    }

    @Test
    public void testSignOut() {
        UserManager.getInstance().setCurrentUser(null);
        assertNull(UserManager.getInstance().getCurrentUser());
    }
}
