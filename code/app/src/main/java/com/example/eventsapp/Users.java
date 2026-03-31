package com.example.eventsapp;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a user in the EventsApp system.
 * A user can be an entrant, organizer, or admin.
 * This class stores user profile information, account settings, and event-related lists.
 */
public class Users implements Serializable {
    private String id;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phoneNumber;
    private String deviceId;
    private String location;
    private String accountType;
    private String profilePictureUrl;
    private List<UserNotification> notifications = new ArrayList<>();
    private List<String> invitedEvents = new ArrayList<>();
    private List<String> registeredEvents = new ArrayList<>();
    private List<String> declinedInvitations = new ArrayList<>();
    private List<String> waitlistedEvents = new ArrayList<>();
    private boolean notificationsEnabled;

    /**
     * No-argument constructor required for Firestore deserialization.
     */
    public Users() {}

    /**
     * Constructor for creating a user with basic info.
     * @param name The full name of the user.
     * @param email The email address of the user.
     * @param password The password for the user's account.
     * @param phoneNumber The phone number of the user.
     */
    public Users(String name, String email, String password, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Constructor used for initializing a user session.
     * @param firstName User's first name.
     * @param lastName User's last name.
     * @param email User's email.
     * @param location User's location.
     * @param accountType User's account type (e.g., "Entrant", "Organizer", "Admin").
     * @param notificationsEnabled Whether the user has enabled notifications.
     */
    public Users(String firstName, String lastName, String email, String location, String accountType, boolean notificationsEnabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = firstName + " " + lastName;
        this.email = email;
        this.location = location;
        this.accountType = accountType;
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * Constructor used during the sign-up process.
     * @param firstName User's first name.
     * @param lastName User's last name.
     * @param email User's email.
     * @param password User's chosen password.
     * @param phoneNumber User's phone number.
     * @param deviceId Unique ID of the device used for sign-up.
     */
    public Users(String firstName, String lastName, String email, String password, String phoneNumber, String deviceId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = firstName + " " + lastName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.deviceId = deviceId;
        this.accountType = "Entrant";
    }

    /**
     * Gets the unique document ID from Firestore.
     * @return The user's ID.
     */
    @Exclude
    public String getId() {
        return id;
    }

    /**
     * Sets the unique document ID from Firestore.
     * @param id The user's ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the full name of the user.
     * @return The full name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the full name of the user.
     * @param name The full name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the email address of the user.
     * @return The email address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the user.
     * @param email The email address.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's password.
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user's password.
     * @param password The password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the user's phone number.
     * @return The phone number as a String.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the user's phone number. Handles both String and Long types from Firestore.
     * @param phoneNumber The phone number object.
     */
    public void setPhoneNumber(Object phoneNumber) {
        if (phoneNumber == null) {
            this.phoneNumber = null;
        } else {
            this.phoneNumber = String.valueOf(phoneNumber);
        }
    }

    /**
     * Gets the unique device ID associated with the user.
     * @return The device ID.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the unique device ID associated with the user.
     * @param deviceId The device ID.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Validates user credentials.
     * @param email The email to check.
     * @param password The password to check.
     * @return True if credentials match, false otherwise.
     */
    public boolean login(String email, String password) {
        return email != null && email.equals(this.email) && password != null && password.equals(this.password);
    }

    /**
     * Gets the first name of the user.
     * @return The first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name of the user.
     * @param firstName The first name.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the last name of the user.
     * @return The last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name of the user.
     * @param lastName The last name.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the full name constructed from first and last name if available.
     * @return The full name.
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return name;
    }

    /**
     * Gets the user's location.
     * @return The location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the user's location.
     * @param location The location.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the account type (e.g., Entrant, Organizer, Admin).
     * @return The account type.
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * Sets the account type.
     * @param accountType The account type.
     */
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    /**
     * Checks if notifications are enabled for the user.
     * @return True if enabled, false otherwise.
     */
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    /**
     * Sets whether notifications are enabled.
     * @param notificationsEnabled True to enable, false to disable.
     */
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * Adds a notification to the user's notification list.
     * @param notification The notification to add.
     */
    public void addNotification(UserNotification notification) {
        if (notifications == null) notifications = new ArrayList<>();
        notifications.add(0, notification);
    }

    /**
     * Removes a notification from the user's list.
     * @param notification The notification to remove.
     */
    public void removeNotification(UserNotification notification) {
        if (notifications != null) {
            notifications.remove(notification);
        }
    }

    /**
     * Gets an unmodifiable list of the user's notifications.
     * @return The list of notifications.
     */
    public List<UserNotification> getNotifications() {
        return notifications != null ? Collections.unmodifiableList(notifications) : new ArrayList<>();
    }

    /**
     * Sets the user's notification list.
     * @param notifications The list of notifications.
     */
    public void setNotifications(List<UserNotification> notifications) {
        this.notifications = notifications;
    }

    /**
     * Adds an event name to the user's invited events list and removes it from waitlist if present.
     * @param eventName The name of the event.
     */
    public void addInvitedEvent(String eventName) {
        if (waitlistedEvents != null) waitlistedEvents.remove(eventName);
        if (invitedEvents == null) invitedEvents = new ArrayList<>();
        addUnique(invitedEvents, eventName);
    }

    /**
     * Adds an event name to the user's waitlisted events list.
     * @param eventName The name of the event.
     */
    public void addWaitlistedEvent(String eventName) {
        if (waitlistedEvents == null) waitlistedEvents = new ArrayList<>();
        addUnique(waitlistedEvents, eventName);
    }

    /**
     * Removes an event name from the user's waitlist.
     * @param eventName The name of the event.
     */
    public void removeWaitlistedEvent(String eventName) {
        if (waitlistedEvents != null) {
            waitlistedEvents.remove(eventName);
        }
    }

    /**
     * Moves an event from invited/waitlisted to registered status.
     * @param eventName The name of the event.
     */
    public void acceptInvitation(String eventName) {
        if (invitedEvents != null) invitedEvents.remove(eventName);
        if (waitlistedEvents != null) waitlistedEvents.remove(eventName);
        if (registeredEvents == null) registeredEvents = new ArrayList<>();
        addUnique(registeredEvents, eventName);
    }

    /**
     * Moves an event from invited status to declined status.
     * @param eventName The name of the event.
     */
    public void declineInvitation(String eventName) {
        if (invitedEvents != null) invitedEvents.remove(eventName);
        if (declinedInvitations == null) declinedInvitations = new ArrayList<>();
        addUnique(declinedInvitations, eventName);
    }

    /**
     * Gets the list of events the user has been invited to.
     * @return Unmodifiable list of invited event names.
     */
    public List<String> getInvitedEvents() {
        return invitedEvents != null ? Collections.unmodifiableList(invitedEvents) : new ArrayList<>();
    }

    /**
     * Sets the list of invited events.
     * @param invitedEvents List of event names.
     */
    public void setInvitedEvents(List<String> invitedEvents) {
        this.invitedEvents = invitedEvents;
    }

    /**
     * Gets the list of events the user is registered for.
     * @return Unmodifiable list of registered event names.
     */
    public List<String> getRegisteredEvents() {
        return registeredEvents != null ? Collections.unmodifiableList(registeredEvents) : new ArrayList<>();
    }

    /**
     * Sets the list of registered events.
     * @param registeredEvents List of event names.
     */
    public void setRegisteredEvents(List<String> registeredEvents) {
        this.registeredEvents = registeredEvents;
    }

    /**
     * Gets the list of invitations the user has declined.
     * @return Unmodifiable list of declined invitation event names.
     */
    public List<String> getDeclinedInvitations() {
        return declinedInvitations != null ? Collections.unmodifiableList(declinedInvitations) : new ArrayList<>();
    }

    /**
     * Sets the list of declined invitations.
     * @param declinedInvitations List of event names.
     */
    public void setDeclinedInvitations(List<String> declinedInvitations) {
        this.declinedInvitations = declinedInvitations;
    }

    /**
     * Gets the list of events the user is currently waitlisted for.
     * @return Unmodifiable list of waitlisted event names.
     */
    public List<String> getWaitlistedEvents() {
        return waitlistedEvents != null ? Collections.unmodifiableList(waitlistedEvents) : new ArrayList<>();
    }

    /**
     * Sets the list of waitlisted events.
     * @param waitlistedEvents List of event names.
     */
    public void setWaitlistedEvents(List<String> waitlistedEvents) {
        this.waitlistedEvents = waitlistedEvents;
    }

    /**
     * Utility method to add an item to a list only if it's not already present.
     * @param events The list to add to.
     * @param eventName The item to add.
     */
    private void addUnique(List<String> events, String eventName) {
        if (events == null) return;
        if (!events.contains(eventName)) {
            events.add(eventName);
        }
    }
}
