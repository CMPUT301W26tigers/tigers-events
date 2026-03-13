package com.example.eventsapp;


/**
 * Represents a notification sent to a user in the event application.
 *
 * Each notification contains:
 * -title: short heading describing the notification
 * -message: detailed notification text
 * -eventId: identifier of the related event
 * -type: category of notification (e.g., invitation, update, reminder)
 * -read: whether the notification has been viewed by the user
 */
public class NotificationItem {
    private String title;
    private String message;
    private String eventId;
    private String type;
    private boolean read;

    public NotificationItem() {
    }
    /**
     * Creates a new notification.
     *
     * @param title   title of the notification
     * @param message notification message content
     * @param eventId ID of the related event
     * @param type    type of notification (e.g., invitation, update)
     * @param read    indicates whether the notification has been read
     */
    public NotificationItem(String title, String message, String eventId, String type, boolean read) {
        this.title = title;
        this.message = message;
        this.eventId = eventId;
        this.type = type;
        this.read = read;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public boolean isRead() {
        return read;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
