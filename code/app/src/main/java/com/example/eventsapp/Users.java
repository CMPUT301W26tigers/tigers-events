package com.example.eventsapp;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Users {
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
    private List<UserNotification> notifications = new ArrayList<>();
    private List<String> invitedEvents = new ArrayList<>();
    private List<String> registeredEvents = new ArrayList<>();
    private List<String> declinedInvitations = new ArrayList<>();
    private List<String> waitlistedEvents = new ArrayList<>();
    private boolean notificationsEnabled;

    // No-argument constructor required for Firestore
    public Users() {}

    // Constructor used in MainActivity
    public Users(String name, String email, String password, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    // Constructor used in UserSession
    public Users(String firstName, String lastName, String email, String location, String accountType, boolean notificationsEnabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = firstName + " " + lastName;
        this.email = email;
        this.location = location;
        this.accountType = accountType;
        this.notificationsEnabled = notificationsEnabled;
    }

    // Constructor used for sign-up
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

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean login(String email, String password) {
        return email != null && email.equals(this.email) && password != null && password.equals(this.password);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public void addNotification(UserNotification notification) {
        if (notifications == null) notifications = new ArrayList<>();
        notifications.add(0, notification);
    }

    public void removeNotification(UserNotification notification) {
        if (notifications != null) {
            notifications.remove(notification);
        }
    }

    public List<UserNotification> getNotifications() {
        return notifications != null ? Collections.unmodifiableList(notifications) : new ArrayList<>();
    }

    public void setNotifications(List<UserNotification> notifications) {
        this.notifications = notifications;
    }

    public void addInvitedEvent(String eventName) {
        if (waitlistedEvents != null) waitlistedEvents.remove(eventName);
        if (invitedEvents == null) invitedEvents = new ArrayList<>();
        addUnique(invitedEvents, eventName);
    }

    public void addWaitlistedEvent(String eventName) {
        if (waitlistedEvents == null) waitlistedEvents = new ArrayList<>();
        addUnique(waitlistedEvents, eventName);
    }

    public void removeWaitlistedEvent(String eventName) {
        if (waitlistedEvents != null) {
            waitlistedEvents.remove(eventName);
        }
    }

    public void acceptInvitation(String eventName) {
        if (invitedEvents != null) invitedEvents.remove(eventName);
        if (waitlistedEvents != null) waitlistedEvents.remove(eventName);
        if (registeredEvents == null) registeredEvents = new ArrayList<>();
        addUnique(registeredEvents, eventName);
    }

    public void declineInvitation(String eventName) {
        if (invitedEvents != null) invitedEvents.remove(eventName);
        if (declinedInvitations == null) declinedInvitations = new ArrayList<>();
        addUnique(declinedInvitations, eventName);
    }

    public List<String> getInvitedEvents() {
        return invitedEvents != null ? Collections.unmodifiableList(invitedEvents) : new ArrayList<>();
    }

    public void setInvitedEvents(List<String> invitedEvents) {
        this.invitedEvents = invitedEvents;
    }

    public List<String> getRegisteredEvents() {
        return registeredEvents != null ? Collections.unmodifiableList(registeredEvents) : new ArrayList<>();
    }

    public void setRegisteredEvents(List<String> registeredEvents) {
        this.registeredEvents = registeredEvents;
    }

    public List<String> getDeclinedInvitations() {
        return declinedInvitations != null ? Collections.unmodifiableList(declinedInvitations) : new ArrayList<>();
    }

    public void setDeclinedInvitations(List<String> declinedInvitations) {
        this.declinedInvitations = declinedInvitations;
    }

    public List<String> getWaitlistedEvents() {
        return waitlistedEvents != null ? Collections.unmodifiableList(waitlistedEvents) : new ArrayList<>();
    }

    public void setWaitlistedEvents(List<String> waitlistedEvents) {
        this.waitlistedEvents = waitlistedEvents;
    }

    private void addUnique(List<String> events, String eventName) {
        if (events == null) return;
        if (!events.contains(eventName)) {
            events.add(eventName);
        }
    }
}
