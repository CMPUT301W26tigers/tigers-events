package com.example.eventsapp;


/**
 * Represents a notification sent to a user in the event application.
 *
 * Each notification contains:
 * -title: short heading describing the notification
 * -message: detailed notification text
 * -eventId: identifier of the related event
 * -eventName: name of the related event
 * -type: category of notification (e.g., invitation, update, reminder)
 * -read: whether the notification has been viewed by the user
 */
public class NotificationItem {
    private String title;
    private String message;
    private String eventId;
    private String eventName;
    private String type;
    private boolean read;

    /**
     * No-argument constructor required for Firestore deserialization.
     */
    public NotificationItem() {
    }

    /**
     * Constructs a NotificationItem with full details.
     * @param title The title of the notification.
     * @param message The detailed message of the notification.
     * @param eventId The ID of the event related to this notification.
     * @param eventName The name of the event related to this notification.
     * @param type The type of notification (e.g., "invitation", "update").
     * @param read Status indicating if the notification has been read by the user.
     */
    public NotificationItem(String title, String message, String eventId, String eventName, String type, boolean read) {
        this.title = title;
        this.message = message;
        this.eventId = eventId;
        this.eventName = eventName;
        this.type = type;
        this.read = read;
    }

    /**
     * Gets the notification title.
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the notification message.
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the event ID associated with this notification.
     * @return The event ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the event name associated with this notification.
     * @return The event name.
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Gets the type of the notification.
     * @return The notification type.
     */
    public String getType() {
        return type;
    }

    /**
     * Checks if the notification has been read.
     * @return True if read, false otherwise.
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Sets the notification title.
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the notification message.
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sets the event ID associated with this notification.
     * @param eventId The event ID to set.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Sets the event name associated with this notification.
     * @param eventName The event name to set.
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Sets the type of the notification.
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Sets the read status of the notification.
     * @param read True to mark as read, false as unread.
     */
    public void setRead(boolean read) {
        this.read = read;
    }
}
