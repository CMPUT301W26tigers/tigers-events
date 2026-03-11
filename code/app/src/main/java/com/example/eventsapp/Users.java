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

    public List<UserNotification> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }
}
