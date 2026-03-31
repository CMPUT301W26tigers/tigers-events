package com.example.eventsapp;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

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
        upsertNotification(userId, eventId, new NotificationItem(
                "Event waitlisted",
                "You are currently on the waitlist for this event.",
                eventId,
                "waitlisted",
                false
        ));
    }

    public void sendInvitationNotification(String userId, String eventId) {
        upsertNotification(userId, eventId, new NotificationItem(
                "Event Invitation",
                "You were selected from the waitlist for this event.",
                eventId,
                "invitation",
                false
        ));
    }

    public void sendNotSelectedNotification(String userId, String eventId) {
        upsertNotification(userId, eventId, new NotificationItem(
                "Removed from Event",
                "You were not selected from the waitlist for this event.",
                eventId,
                "not_selected",
                false
        ));
    }

    private void upsertNotification(String userId, String eventId, NotificationItem notification) {
        if (isBlank(userId) || isBlank(eventId)) {
            return;
        }

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
}
