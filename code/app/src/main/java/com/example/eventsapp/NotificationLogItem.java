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

    public NotificationLogItem() {
    }

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

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public List<Map<String, String>> getRecipients() { return recipients; }
    public void setRecipients(List<Map<String, String>> recipients) { this.recipients = recipients; }
}
