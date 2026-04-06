package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SessionAndCleanupRegressionTest {

    @Test
    public void userManager_isSingleton() {
        assertSame(UserManager.getInstance(), UserManager.getInstance());
    }

    @Test
    public void userManager_loginStateTracksCurrentUser() {
        UserManager manager = UserManager.getInstance();
        manager.logout();

        assertFalse(manager.isLoggedIn());

        Users user = new Users("Test", "User", "test@example.com", "Edmonton, AB", "Entrant", true);
        manager.setCurrentUser(user);

        assertTrue(manager.isLoggedIn());
        assertSame(user, manager.getCurrentUser());

        manager.logout();
        assertFalse(manager.isLoggedIn());
        assertEquals(null, manager.getCurrentUser());
    }

    @Test
    public void userManager_logoutClearsCurrentUser() {
        UserManager manager = UserManager.getInstance();
        manager.setCurrentUser(new Users("Rishawn", "Paramapathy", "rishawn@ualberta.ca",
                "Edmonton, AB", "Entrant", true));

        manager.logout();

        assertFalse(manager.isLoggedIn());
        assertNull(manager.getCurrentUser());
    }

    @Test
    public void userManager_logoutPreservesBootstrapCompletionState() {
        UserManager manager = UserManager.getInstance();
        manager.setCurrentUser(new Users("Rishawn", "Paramapathy", "rishawn@ualberta.ca",
                "Edmonton, AB", "Entrant", true));

        manager.logout();

        assertTrue(manager.isSessionBootstrapComplete());
        assertFalse(manager.isSessionBootstrapInProgress());
    }

    @Test
    public void calculateBatchCount_zeroOrNegative_returnsZero() {
        assertEquals(0, EventCleanupHelper.calculateBatchCount(0));
        assertEquals(0, EventCleanupHelper.calculateBatchCount(-3));
    }

    @Test
    public void calculateBatchCount_splitsDeletesAtBatchBoundary() {
        assertEquals(1, EventCleanupHelper.calculateBatchCount(1));
        assertEquals(1, EventCleanupHelper.calculateBatchCount(450));
        assertEquals(2, EventCleanupHelper.calculateBatchCount(451));
        assertEquals(3, EventCleanupHelper.calculateBatchCount(901));
    }
}
