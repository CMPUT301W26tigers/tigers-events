package com.example.eventsapp;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * POJO representing a document in the global {@code notification_logs} Firestore collection.
 * Each document groups all recipients of the same notification type for the same event.
 */
public class NotificationLogItem {
    private String organizerName;
    private String organizerId;
    private String title;
    private String message;
    private String eventId;
    private String eventName;
    private String type;
    private Timestamp timestamp;
    private List<Map<String, String>> recipients;

    /**
     * No-argument constructor required for Firestore deserialization.
     */
    public NotificationLogItem() {
    }

    /**
     * Constructs a fully populated {@code NotificationLogItem}.
     *
     * @param organizerName the display name of the organizer who sent the notification
     * @param organizerId   the Firestore document ID of the organizer
     * @param title         the notification title shown to recipients
     * @param message       the notification body shown to recipients
     * @param eventId       the Firestore document ID of the related event
     * @param eventName     the human-readable name of the related event
     * @param type          the notification category (e.g., "INVITATION", "UPDATE")
     * @param timestamp     the Firestore server timestamp when the notification was sent
     * @param recipients    a list of recipient maps; each map contains at minimum a {@code "name"}
     *                      and {@code "userId"} entry
     */
    public NotificationLogItem(String organizerName, String organizerId, String title,
                               String message, String eventId, String eventName, String type,
                               Timestamp timestamp, List<Map<String, String>> recipients) {
        this.organizerName = organizerName;
        this.organizerId = organizerId;
        this.title = title;
        this.message = message;
        this.eventId = eventId;
        this.eventName = eventName;
        this.type = type;
        this.timestamp = timestamp;
        this.recipients = recipients;
    }

    /**
     * Extracts and returns just the display names from the {@code recipients} list.
     * Entries with a {@code null} or empty {@code "name"} value are silently skipped.
     *
     * @return a mutable list of recipient display names; never {@code null}
     */
    public List<String> getRecipientNames() {
        List<String> names = new ArrayList<>();
        if (recipients != null) {
            for (Map<String, String> r : recipients) {
                String name = r.get("name");
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /** @return the display name of the organizer who sent this notification */
    public String getOrganizerName() { return organizerName; }
    /** @param organizerName the display name of the organizer */
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    /** @return the Firestore document ID of the organizer */
    public String getOrganizerId() { return organizerId; }
    /** @param organizerId the Firestore document ID of the organizer */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    /** @return the notification title shown to recipients */
    public String getTitle() { return title; }
    /** @param title the notification title */
    public void setTitle(String title) { this.title = title; }

    /** @return the notification body shown to recipients */
    public String getMessage() { return message; }
    /** @param message the notification body */
    public void setMessage(String message) { this.message = message; }

    /** @return the Firestore document ID of the related event */
    public String getEventId() { return eventId; }
    /** @param eventId the Firestore document ID of the related event */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** @return the human-readable name of the related event */
    public String getEventName() { return eventName; }
    /** @param eventName the human-readable event name */
    public void setEventName(String eventName) { this.eventName = eventName; }

    /** @return the notification category (e.g., {@code "INVITATION"}, {@code "UPDATE"}) */
    public String getType() { return type; }
    /** @param type the notification category */
    public void setType(String type) { this.type = type; }

    /** @return the Firestore server timestamp recording when the notification was dispatched */
    public Timestamp getTimestamp() { return timestamp; }
    /** @param timestamp the Firestore server timestamp of dispatch */
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    /**
     * Returns the raw recipients list where each element is a map containing at minimum
     * {@code "name"} and {@code "userId"} entries.
     *
     * @return the recipients list, or {@code null} if not yet set
     */
    public List<Map<String, String>> getRecipients() { return recipients; }
    /**
     * Replaces the recipients list.
     *
     * @param recipients a list of recipient maps; each map should contain at minimum
     *                   {@code "name"} and {@code "userId"} entries
     */
    public void setRecipients(List<Map<String, String>> recipients) { this.recipients = recipients; }
}
