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

    /**
     * Creates a helper backed by the default {@link FirebaseFirestore} instance.
     */
    public FirestoreNotificationHelper() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Creates a helper backed by the provided Firestore instance.
     * Intended for dependency injection and testing.
     *
     * @param db the Firestore instance to use for all reads and writes
     */
    FirestoreNotificationHelper(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Sends an optional "waitlisted" notification to the user.
     * The notification is suppressed if the user has disabled optional notifications.
     *
     * @param userId  the Firestore document ID of the recipient
     * @param eventId the Firestore document ID of the event
     */
    public void sendWaitlistedNotification(String userId, String eventId) {
        sendOptionalNotification(userId, eventId,
                "Event waitlisted",
                "You are currently on the waitlist for this event.",
                "waitlisted");
    }

    /**
     * Sends a critical "lottery invitation" notification to the user indicating they were
     * selected from the waitlist. Critical notifications bypass the user's notification
     * preference setting and are always delivered.
     *
     * @param userId  the Firestore document ID of the recipient
     * @param eventId the Firestore document ID of the event
     */
    public void sendInvitationNotification(String userId, String eventId) {
        sendCriticalNotification(userId, eventId,
                "Event Invitation",
                "You were selected from the waitlist for this event.",
                "invitation");
    }

    /**
     * Sends a critical notification informing the user that they have been invited to join
     * the waitlist of a private event. Bypasses the user's optional notification preference.
     *
     * @param userId  the Firestore document ID of the recipient
     * @param eventId the Firestore document ID of the private event
     */
    public void sendPrivateWaitlistInvitationNotification(String userId, String eventId) {
        sendCriticalNotification(userId, eventId,
                "Private Event Invitation",
                "You have been invited to join the waiting list for a private event.",
                "private_waitlist_invitation");
    }

    /**
     * Sends a critical notification inviting the user to become a co-organizer of the event.
     * Bypasses the user's optional notification preference.
     *
     * @param userId  the Firestore document ID of the invited user
     * @param eventId the Firestore document ID of the event
     */
    public void sendCoOrganizerInvitationNotification(String userId, String eventId) {
        sendCriticalNotification(userId, eventId,
                "Co-organizer Invitation",
                "You have been invited to help organize this event.",
                "co_organizer_invitation");
    }

    /**
     * Sends an optional notification informing the user that they were not selected from the
     * waitlist. Suppressed if the user has disabled optional notifications.
     *
     * @param userId  the Firestore document ID of the recipient
     * @param eventId the Firestore document ID of the event
     */
    public void sendNotSelectedNotification(String userId, String eventId) {
        sendOptionalNotification(userId, eventId,
                "Removed from Event",
                "You were not selected from the waitlist for this event.",
                "not_selected");
    }

    private void sendOptionalNotification(String userId, String eventId,
                                          String title, String message, String type) {
        if (isBlank(userId) || isBlank(eventId)) {
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    Boolean enabled = userDoc.getBoolean("notificationsEnabled");

                    // Missing field defaults to enabled
                    if (enabled != null && !enabled) {
                        return;
                    }

                    upsertNotification(userId, eventId, title, message, type);
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Failed to check notification preference", e));
    }

    private void sendCriticalNotification(String userId, String eventId,
                                          String title, String message, String type) {
        if (isBlank(userId) || isBlank(eventId)) {
            return;
        }

        upsertNotification(userId, eventId, title, message, type);
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

    /**
     * Builds a deterministic Firestore document ID for a notification of the given type and
     * event, in the form {@code <type>_<eventId>}.
     *
     * <p>Using a deterministic ID means re-sending the same notification type for the same
     * event overwrites the existing document rather than creating a duplicate.
     *
     * @param type    the notification type string (e.g. {@code "invitation"}); if {@code null},
     *                {@code "notification"} is used
     * @param eventId the Firestore document ID of the event
     * @return a non-null document ID string
     */
    @NonNull
    static String buildNotificationDocumentId(String type, String eventId) {
        return (type == null ? "notification" : type) + "_" + eventId;
    }

    /**
     * Attempts to derive a human-readable display name from a Firestore user document.
     *
     * <p>Resolution order: {@code name} field → concatenation of {@code firstName} and
     * {@code lastName} fields → {@code "Unknown"} when none are present.
     *
     * @param userDoc the Firestore user document snapshot
     * @return a non-null, non-empty display name string
     */
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
