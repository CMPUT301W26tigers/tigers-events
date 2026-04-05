package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
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
    public void userSession_resetDemoUserRestoresExpectedNotifications() {
        UserSession.resetDemoUser();

        Users demoUser = UserSession.getCurrentUser();
        assertNotNull(demoUser);
        assertEquals("Rishawn Paramapathy", demoUser.getName());
        assertEquals(2, demoUser.getNotifications().size());
        assertEquals("Event Invitation", demoUser.getNotifications().get(0).getTitle());
        assertEquals("Event waitlisted", demoUser.getNotifications().get(1).getTitle());
    }

    @Test
    public void userSession_resetDemoUserReplacesMutatedSessionState() {
        UserSession.resetDemoUser();
        Users original = UserSession.getCurrentUser();
        original.acceptInvitation("Campus Startup Showcase");
        original.removeNotification(original.getNotifications().get(0));

        UserSession.resetDemoUser();
        Users reset = UserSession.getCurrentUser();

        assertNotNull(reset);
        assertEquals(2, reset.getNotifications().size());
        assertTrue(reset.getInvitedEvents().contains("Campus Startup Showcase"));
        assertFalse(reset.getRegisteredEvents().contains("Campus Startup Showcase"));
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
