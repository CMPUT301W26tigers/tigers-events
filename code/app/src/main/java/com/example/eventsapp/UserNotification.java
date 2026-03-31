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
        /** Invitation to join a private event waitlist. */
        PRIVATE_WAITLIST_INVITATION,
        /** Invitation to become a co-organizer for an event. */
        CO_ORGANIZER_INVITATION,
        /** Status update indicating the user is still waitlisted. */
        WAITLISTED,
        /** Notification that the user was not selected in the lottery. */
        NOT_SELECTED
    }

    private final Type type;
    private final String title;
    private final String eventName;
    private final String message;
    private final String eventId;
    private final String notificationId;

    /**
     * Constructs a UserNotification.
     * @param type The type of notification.
     * @param title The title string.
     * @param eventName The name of the event associated with the notification.
     * @param message The descriptive message.
     */
    public UserNotification(Type type, String title, String eventName, String message) {
        this(type, title, eventName, message, null, null);
    }

    /**
     * Constructs a UserNotification with Firestore metadata for real event-backed inbox items.
     * @param type The type of notification.
     * @param title The title string.
     * @param eventName The name of the event associated with the notification.
     * @param message The descriptive message.
     * @param eventId The Firestore event ID associated with the notification.
     * @param notificationId The Firestore document ID for the notification.
     */
    public UserNotification(Type type, String title, String eventName, String message, String eventId, String notificationId) {
        this.type = type;
        this.title = title;
        this.eventName = eventName;
        this.message = message;
        this.eventId = eventId;
        this.notificationId = notificationId;
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

    /**
     * Gets the Firestore event ID associated with this notification, if any.
     * @return The event ID or null.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the Firestore document ID for this notification, if any.
     * @return The notification document ID or null.
     */
    public String getNotificationId() {
        return notificationId;
    }
}
