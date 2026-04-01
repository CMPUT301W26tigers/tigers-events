package com.example.eventsapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for writing inbox notifications to Firestore for real event activity.
 * Also writes to a global {@code notification_logs} collection for admin visibility.
 */
public class FirestoreNotificationHelper {
    private static final String TAG = "NotificationHelper";
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
                .addOnSuccessListener(eventDoc -> {
                    String eventName = resolveEventName(eventDoc);
                    String createdBy = eventDoc.getString("createdBy");

                    // Write per-user notification immediately (existing behavior)
                    writeNotification(userId, eventId,
                            new NotificationItem(title, message, eventId, eventName, type, false));

                    // Write to global notification_logs for admin visibility
                    writeNotificationLog(userId, eventId, type, title, message, eventName, createdBy);
                })
                .addOnFailureListener(unused -> {
                    writeNotification(userId, eventId,
                            new NotificationItem(title, message, eventId, "", type, false));

                    // Still write log even if event doc fetch fails
                    writeNotificationLog(userId, eventId, type, title, message, "", null);
                });
    }

    private void writeNotification(String userId, String eventId, NotificationItem notification) {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .document(buildNotificationDocumentId(notification.getType(), eventId))
                .set(notification);
    }

    /**
     * Writes to the global notification_logs collection. Resolves organizer and recipient names
     * before writing. Uses set-with-merge and arrayUnion so recipients accumulate.
     */
    private void writeNotificationLog(String userId, String eventId, String type,
                                      String title, String message, String eventName,
                                      String createdBy) {
        // Look up recipient name
        db.collection("users").document(userId).get()
                .addOnSuccessListener(recipientDoc -> {
                    String recipientName = resolveUserName(recipientDoc);

                    if (!isBlank(createdBy)) {
                        // Look up organizer name
                        db.collection("users").document(createdBy).get()
                                .addOnSuccessListener(organizerDoc -> {
                                    String organizerName = resolveUserName(organizerDoc);
                                    writeLogDocument(eventId, type, title, message, eventName,
                                            createdBy, organizerName, userId, recipientName);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to resolve organizer name", e);
                                    writeLogDocument(eventId, type, title, message, eventName,
                                            createdBy, "Unknown", userId, recipientName);
                                });
                    } else {
                        writeLogDocument(eventId, type, title, message, eventName,
                                "", "Unknown", userId, recipientName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to resolve recipient name", e);
                    if (!isBlank(createdBy)) {
                        db.collection("users").document(createdBy).get()
                                .addOnSuccessListener(organizerDoc -> {
                                    String organizerName = resolveUserName(organizerDoc);
                                    writeLogDocument(eventId, type, title, message, eventName,
                                            createdBy, organizerName, userId, "Unknown");
                                })
                                .addOnFailureListener(e2 -> writeLogDocument(eventId, type, title,
                                        message, eventName, createdBy, "Unknown", userId, "Unknown"));
                    } else {
                        writeLogDocument(eventId, type, title, message, eventName,
                                "", "Unknown", userId, "Unknown");
                    }
                });
    }

    private void writeLogDocument(String eventId, String type, String title, String message,
                                  String eventName, String organizerId, String organizerName,
                                  String recipientUserId, String recipientName) {
        String docId = buildNotificationDocumentId(type, eventId);

        Map<String, String> recipientEntry = new HashMap<>();
        recipientEntry.put("userId", recipientUserId);
        recipientEntry.put("name", recipientName);

        Map<String, Object> logData = new HashMap<>();
        logData.put("organizerName", organizerName);
        logData.put("organizerId", organizerId);
        logData.put("title", title);
        logData.put("message", message);
        logData.put("eventId", eventId);
        logData.put("eventName", eventName);
        logData.put("type", type);
        logData.put("timestamp", FieldValue.serverTimestamp());
        logData.put("recipients", FieldValue.arrayUnion(recipientEntry));

        db.collection("notification_logs").document(docId)
                .set(logData, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Failed to write notification log", e));
    }

    @NonNull
    static String buildNotificationDocumentId(String type, String eventId) {
        return (type == null ? "notification" : type) + "_" + eventId;
    }

    @NonNull
    String resolveUserName(@NonNull DocumentSnapshot userDoc) {
        String name = userDoc.getString("name");
        if (!isBlank(name)) {
            return name.trim();
        }
        String first = userDoc.getString("firstName");
        String last = userDoc.getString("lastName");
        if (!isBlank(first) || !isBlank(last)) {
            return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        }
        return "Unknown";
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
