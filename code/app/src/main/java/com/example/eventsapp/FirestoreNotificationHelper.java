package com.example.eventsapp;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Helper for writing inbox notifications to Firestore for real event activity.
 */
public class FirestoreNotificationHelper {
    private final FirebaseFirestore db;

    public FirestoreNotificationHelper() {
        this(FirebaseFirestore.getInstance());
    }

    FirestoreNotificationHelper(FirebaseFirestore db) {
        this.db = db;
    }

    public void sendWaitlistedNotification(String userId, String eventId) {
        upsertNotification(userId, eventId,
                "Event waitlisted",
                "You are currently on the waitlist for this event.",
                "waitlisted");
    }

    public void sendInvitationNotification(String userId, String eventId) {
        upsertNotification(userId, eventId,
                "Event Invitation",
                "You were selected from the waitlist for this event.",
                "invitation");
    }

    public void sendPrivateWaitlistInvitationNotification(String userId, String eventId) {
        upsertNotification(userId, eventId,
                "Private Event Invitation",
                "You have been invited to join the waiting list for a private event.",
                "private_waitlist_invitation");
    }

    public void sendCoOrganizerInvitationNotification(String userId, String eventId) {
        upsertNotification(userId, eventId,
                "Co-organizer Invitation",
                "You have been invited to help organize this event.",
                "co_organizer_invitation");
    }

    public void sendNotSelectedNotification(String userId, String eventId) {
        upsertNotification(userId, eventId,
                "Removed from Event",
                "You were not selected from the waitlist for this event.",
                "not_selected");
    }

    private void upsertNotification(String userId, String eventId, String title, String message, String type) {
        if (isBlank(userId) || isBlank(eventId)) {
            return;
        }

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> writeNotification(
                        userId,
                        eventId,
                        new NotificationItem(
                                title,
                                message,
                                eventId,
                                resolveEventName(eventDoc),
                                type,
                                false
                        )
                ))
                .addOnFailureListener(unused -> writeNotification(
                        userId,
                        eventId,
                        new NotificationItem(title, message, eventId, "", type, false)
                ));
    }

    private void writeNotification(String userId, String eventId, NotificationItem notification) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(buildNotificationDocumentId(notification.getType(), eventId))
                .set(notification);
    }

    @NonNull
    static String buildNotificationDocumentId(String type, String eventId) {
        return (type == null ? "notification" : type) + "_" + eventId;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @NonNull
    private String resolveEventName(@NonNull DocumentSnapshot eventDoc) {
        String eventName = eventDoc.getString("name");
        if (isBlank(eventName)) {
            eventName = eventDoc.getString("eventName");
        }
        if (isBlank(eventName)) {
            eventName = eventDoc.getString("title");
        }
        return eventName == null ? "" : eventName.trim();
    }
}
