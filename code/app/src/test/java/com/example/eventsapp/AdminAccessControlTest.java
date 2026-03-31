package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the admin access control logic used in AccountFragment.
 * The Management Tools button should only be visible when accountType == "Admin".
 */
public class AdminAccessControlTest {

    /**
     * Replicates the visibility check from AccountFragment:
     * if (user != null && "Admin".equalsIgnoreCase(user.getAccountType()))
     */
    private boolean shouldShowManagementTools(Users user) {
        return user != null && "Admin".equalsIgnoreCase(user.getAccountType());
    }

    @Test
    public void managementTools_adminUser_isVisible() {
        Users user = new Users("Admin", "User", "admin@test.com", "Edmonton, AB", "Admin", true);
        assertTrue(shouldShowManagementTools(user));
    }

    @Test
    public void managementTools_adminLowercase_isVisible() {
        Users user = new Users();
        user.setAccountType("admin");
        assertTrue(shouldShowManagementTools(user));
    }

    @Test
    public void managementTools_adminMixedCase_isVisible() {
        Users user = new Users();
        user.setAccountType("ADMIN");
        assertTrue(shouldShowManagementTools(user));
    }

    @Test
    public void managementTools_entrantUser_isHidden() {
        Users user = new Users("Regular", "User", "user@test.com", "Edmonton, AB", "Entrant", true);
        assertFalse(shouldShowManagementTools(user));
    }

    @Test
    public void managementTools_organizerUser_isHidden() {
        Users user = new Users();
        user.setAccountType("Organizer");
        assertFalse(shouldShowManagementTools(user));
    }

    @Test
    public void managementTools_nullAccountType_isHidden() {
        Users user = new Users();
        assertFalse(shouldShowManagementTools(user));
    }

    @Test
    public void managementTools_nullUser_isHidden() {
        assertFalse(shouldShowManagementTools(null));
    }

    @Test
    public void managementTools_emptyAccountType_isHidden() {
        Users user = new Users();
        user.setAccountType("");
        assertFalse(shouldShowManagementTools(user));
    }
}
