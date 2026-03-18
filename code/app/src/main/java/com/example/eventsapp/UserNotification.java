package com.example.eventsapp;

/**
 * Represents a notification intended for a user within the application's UI.
 * This class is used for runtime notification handling and display in the user's inbox.
 */
public class UserNotification {

    /**
     * Enumeration of notification types.
     */
    public enum Type {
        /** Invitation to register for an event. */
        INVITATION,
        /** Status update indicating the user is still waitlisted. */
        WAITLISTED,
        /** Notification that the user was not selected in the lottery. */
        NOT_SELECTED
    }

    private final Type type;
    private final String title;
    private final String eventName;
    private final String message;

    /**
     * Constructs a UserNotification.
     * @param type The type of notification.
     * @param title The title string.
     * @param eventName The name of the event associated with the notification.
     * @param message The descriptive message.
     */
    public UserNotification(Type type, String title, String eventName, String message) {
        this.type = type;
        this.title = title;
        this.eventName = eventName;
        this.message = message;
    }

    /**
     * Gets the notification type.
     * @return The type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the notification title.
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the name of the event this notification refers to.
     * @return The event name.
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Gets the detailed notification message.
     * @return The message.
     */
    public String getMessage() {
        return message;
    }
}
