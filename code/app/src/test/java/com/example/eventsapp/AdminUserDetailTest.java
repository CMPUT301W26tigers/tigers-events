package com.example.eventsapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the display logic used in AdminUserDetailBottomSheet and AdminUserAdapter.
 * Covers display name resolution, avatar initials, profile picture, and account type handling.
 */
public class AdminUserDetailTest {

    // Display name resolution (mirrors AdminUserDetailBottomSheet logic)

    private String resolveDisplayName(Users user) {
        String displayName = user.getName();
        if (displayName == null || displayName.isEmpty()) {
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            displayName = (first + " " + last).trim();
        }
        if (displayName.isEmpty()) displayName = "Unknown";
        return displayName;
    }

    @Test
    public void displayName_withNameField_usesNameField() {
        Users user = new Users("John Doe", "john@test.com", "pass", "1234567890");
        assertEquals("John Doe", resolveDisplayName(user));
    }

    @Test
    public void displayName_nullName_fallsBackToFirstLastName() {
        Users user = new Users();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        assertEquals("Jane Doe", resolveDisplayName(user));
    }

    @Test
    public void displayName_emptyName_fallsBackToFirstLastName() {
        Users user = new Users();
        user.setName("");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        assertEquals("Jane Doe", resolveDisplayName(user));
    }

    @Test
    public void displayName_allNull_returnsUnknown() {
        Users user = new Users();
        assertEquals("Unknown", resolveDisplayName(user));
    }

    @Test
    public void displayName_onlyFirstName_returnsFirstName() {
        Users user = new Users();
        user.setFirstName("Alice");
        assertEquals("Alice", resolveDisplayName(user));
    }

    @Test
    public void displayName_onlyLastName_returnsLastName() {
        Users user = new Users();
        user.setLastName("Smith");
        assertEquals("Smith", resolveDisplayName(user));
    }

    // Avatar initial (mirrors AdminUserAdapter.onBindViewHolder logic)

    @Test
    public void avatarInitial_isUppercaseFirstLetter() {
        Users user = new Users("alice smith", "alice@test.com", "pass", "1234567890");
        String displayName = resolveDisplayName(user);
        String initial = displayName.substring(0, 1).toUpperCase();
        assertEquals("A", initial);
    }

    @Test
    public void avatarInitial_unknownUser_showsU() {
        Users user = new Users();
        String displayName = resolveDisplayName(user);
        String initial = displayName.substring(0, 1).toUpperCase();
        assertEquals("U", initial);
    }

    // Profile picture URL

    @Test
    public void profilePicture_withUrl_hasDeletionPath() {
        Users user = new Users();
        user.setProfilePictureUrl("http://pic.jpg");
        String url = user.getProfilePictureUrl();
        boolean hasPicture = url != null && !url.isEmpty();
        assertTrue(hasPicture);
    }

    @Test
    public void profilePicture_nullUrl_noDeletionNeeded() {
        Users user = new Users();
        String url = user.getProfilePictureUrl();
        boolean hasPicture = url != null && !url.isEmpty();
        assertFalse(hasPicture);
    }

    @Test
    public void profilePicture_emptyUrl_noDeletionNeeded() {
        Users user = new Users();
        user.setProfilePictureUrl("");
        String url = user.getProfilePictureUrl();
        boolean hasPicture = url != null && !url.isEmpty();
        assertFalse(hasPicture);
    }

    @Test
    public void profilePicture_clearUrl_setsToEmpty() {
        Users user = new Users();
        user.setProfilePictureUrl("http://pic.jpg");
        user.setProfilePictureUrl("");
        assertEquals("", user.getProfilePictureUrl());
    }

    // Account type display (mirrors AdminUserAdapter.onBindViewHolder logic)

    @Test
    public void accountType_withValue_displaysValue() {
        Users user = new Users();
        user.setAccountType("Organizer");
        String display = user.getAccountType() != null ? user.getAccountType() : "User";
        assertEquals("Organizer", display);
    }

    @Test
    public void accountType_null_displaysFallback() {
        Users user = new Users();
        String display = user.getAccountType() != null ? user.getAccountType() : "User";
        assertEquals("User", display);
    }

    @Test
    public void accountType_admin_displaysAdmin() {
        Users user = new Users();
        user.setAccountType("Admin");
        assertEquals("Admin", user.getAccountType());
    }

    // Email and phone display (mirrors AdminUserDetailBottomSheet logic)

    @Test
    public void email_null_showsDash() {
        Users user = new Users();
        String email = user.getEmail() != null ? user.getEmail() : "—";
        assertEquals("—", email);
    }

    @Test
    public void email_withValue_showsEmail() {
        Users user = new Users();
        user.setEmail("test@example.com");
        String email = user.getEmail() != null ? user.getEmail() : "—";
        assertEquals("test@example.com", email);
    }

    @Test
    public void phone_null_showsDash() {
        Users user = new Users();
        String phone = user.getPhoneNumber();
        String display = (phone != null && !phone.isEmpty()) ? phone : "—";
        assertEquals("—", display);
    }

    @Test
    public void phone_withValue_showsPhone() {
        Users user = new Users();
        user.setPhoneNumber("7801234567");
        String phone = user.getPhoneNumber();
        String display = (phone != null && !phone.isEmpty()) ? phone : "—";
        assertEquals("7801234567", display);
    }

    // Delete confirmation name resolution (mirrors AdminUserDetailBottomSheet.showDeleteConfirmation)

    @Test
    public void deleteConfirmationName_nullName_showsThisUser() {
        Users user = new Users();
        String displayName = user.getName();
        if (displayName == null || displayName.isEmpty()) {
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            displayName = (first + " " + last).trim();
        }
        if (displayName.isEmpty()) displayName = "this user";
        assertEquals("this user", displayName);
    }
}
