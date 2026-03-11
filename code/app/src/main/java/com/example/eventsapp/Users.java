package com.example.eventsapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Users {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String location;
    private final String accountType;
    private final List<UserNotification> notifications;
    private final List<String> invitedEvents;
    private final List<String> registeredEvents;
    private final List<String> declinedInvitations;
    private final List<String> waitlistedEvents;
    private boolean notificationsEnabled;

    public Users(String firstName,
                 String lastName,
                 String email,
                 String location,
                 String accountType,
                 boolean notificationsEnabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.location = location;
        this.accountType = accountType;
        this.notificationsEnabled = notificationsEnabled;
        this.notifications = new ArrayList<>();
        this.invitedEvents = new ArrayList<>();
        this.registeredEvents = new ArrayList<>();
        this.declinedInvitations = new ArrayList<>();
        this.waitlistedEvents = new ArrayList<>();
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getLocation() {
        return location;
    }

    public String getAccountType() {
        return accountType;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public void addNotification(UserNotification notification) {
        notifications.add(0, notification);
    }

    public void removeNotification(UserNotification notification) {
        notifications.remove(notification);
    }

    public List<UserNotification> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    public void addInvitedEvent(String eventName) {
        waitlistedEvents.remove(eventName);
        addUnique(invitedEvents, eventName);
    }

    public void addWaitlistedEvent(String eventName) {
        addUnique(waitlistedEvents, eventName);
    }

    public void removeWaitlistedEvent(String eventName) {
        waitlistedEvents.remove(eventName);
    }

    public void acceptInvitation(String eventName) {
        invitedEvents.remove(eventName);
        waitlistedEvents.remove(eventName);
        addUnique(registeredEvents, eventName);
    }

    public void declineInvitation(String eventName) {
        invitedEvents.remove(eventName);
        addUnique(declinedInvitations, eventName);
    }

    public List<String> getInvitedEvents() {
        return Collections.unmodifiableList(invitedEvents);
    }

    public List<String> getRegisteredEvents() {
        return Collections.unmodifiableList(registeredEvents);
    }

    public List<String> getDeclinedInvitations() {
        return Collections.unmodifiableList(declinedInvitations);
    }

    public List<String> getWaitlistedEvents() {
        return Collections.unmodifiableList(waitlistedEvents);
    }

    private void addUnique(List<String> events, String eventName) {
        if (!events.contains(eventName)) {
            events.add(eventName);
        }
    }
}
