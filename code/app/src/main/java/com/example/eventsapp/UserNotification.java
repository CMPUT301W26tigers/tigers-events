package com.example.eventsapp;

public class UserNotification {
    public enum Type {
        INVITATION,
        WAITLISTED,
        NOT_SELECTED
    }

    private final Type type;
    private final String title;
    private final String eventName;
    private final String message;

    public UserNotification(Type type, String title, String eventName, String message) {
        this.type = type;
        this.title = title;
        this.eventName = eventName;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getEventName() {
        return eventName;
    }

    public String getMessage() {
        return message;
    }
}
