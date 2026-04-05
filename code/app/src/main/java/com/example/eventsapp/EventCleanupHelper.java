package com.example.eventsapp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class that handles automatic cleanup of expired events.
 * Events are considered expired 12 hours after their event_date.
 * Before deletion, all participants' eventHistory records are marked as expired.
 */
public class EventCleanupHelper {

    private static final String TAG = "EventCleanupHelper";
    private static final int EXPIRATION_HOURS = 12;
    private static final int MAX_BATCH_DELETES = 450;

    /**
     * Checks all events in Firestore and deletes any that have expired
     * (event_date + 12 hours has passed). Before deleting, marks all
     * participants' eventHistory records as expired.
     */
    public static void cleanupExpiredEvents() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Date now = new Date();

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot eventDoc : querySnapshot) {
                        String eventDate = eventDoc.getString("event_date");
                        if (eventDate == null || eventDate.isEmpty()) continue;

                        if (isExpired(eventDate, now)) {
                            String eventId = eventDoc.getString("id");
                            if (eventId == null) eventId = eventDoc.getId();
                            String createdBy = eventDoc.getString("createdBy");
                            java.util.List<String> coOrganizerIds =
                                    FirestoreDataUtils.getStringList(eventDoc, "coOrganizerIds");
                            markHistoryAndDelete(db, eventId, createdBy, coOrganizerIds);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to query events for cleanup", e));
    }

    /**
     * Checks if an event date + 12 hours has passed relative to the current time.
     *
     * @param eventDateStr The event date string in yyyy-MM-dd format.
     * @param now The current date/time.
     * @return true if the event has expired.
     */
    static boolean isExpired(String eventDateStr, Date now) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        sdf.setLenient(false);
        try {
            Date eventDate = sdf.parse(eventDateStr);
            if (eventDate == null) return false;
            Calendar cal = Calendar.getInstance();
            cal.setTime(eventDate);
            // Event date is start of day; add 24 hours (end of day) + 12 hours = 36 hours
            cal.add(Calendar.HOUR_OF_DAY, 24 + EXPIRATION_HOURS);
            return now.after(cal.getTime());
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Marks all participants' and organizer's eventHistory records as expired,
     * then deletes the event and its entrants from the global collection.
     */
    private static void markHistoryAndDelete(FirebaseFirestore db, String eventId, String createdBy,
                                             java.util.List<String> coOrganizerIds) {
        // Mark organizer's history as expired
        if (createdBy != null && !createdBy.isEmpty()) {
            db.collection("users").document(createdBy)
                    .collection("eventHistory").document(eventId)
                    .update("expired", true)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to mark organizer history expired", e));
        }

        if (coOrganizerIds != null) {
            for (String coOrganizerId : coOrganizerIds) {
                if (coOrganizerId == null || coOrganizerId.isEmpty()) {
                    continue;
                }
                db.collection("users").document(coOrganizerId)
                        .collection("eventHistory").document(eventId)
                        .update("expired", true)
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to mark co-organizer history expired", e));
            }
        }

        // Mark all entrants' history as expired, then delete entrant docs and event
        db.collection("events").document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(entrantSnapshot -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot entrantDoc : entrantSnapshot.getDocuments()) {
                        String userId = entrantDoc.getString("userId");
                        if (userId != null && !userId.isEmpty()) {
                            // Mark participant's history as expired
                            db.collection("users").document(userId)
                                    .collection("eventHistory").document(eventId)
                                    .update("expired", true)
                                    .addOnFailureListener(e ->
                                            Log.w(TAG, "Failed to mark entrant history expired", e));
                        }
                        // Delete entrant doc
                        batch.delete(entrantDoc.getReference());
                    }

                    // Delete the event document
                    batch.delete(db.collection("events").document(eventId));

                    batch.commit()
                            .addOnSuccessListener(v -> Log.d(TAG, "Deleted expired event: " + eventId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to delete expired event", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to query entrants for cleanup", e));
    }

    /**
     * Writes a full event copy to a user's eventHistory subcollection.
     *
     * @param userId The user's Firestore ID.
     * @param eventId The event ID.
     * @param eventData Map of event fields to store.
     * @param entrantStatus The user's status (APPLIED, INVITED, ACCEPTED, DECLINED, CANCELLED, ORGANIZED).
     */
    public static void writeHistoryRecord(String userId, String eventId,
                                           java.util.Map<String, Object> eventData,
                                           String entrantStatus) {
        if (userId == null || eventId == null) return;

        java.util.Map<String, Object> historyData = new java.util.HashMap<>(eventData);
        historyData.put("entrantStatus", entrantStatus);
        historyData.put("expired", false);

        FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("eventHistory").document(eventId)
                .set(historyData)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to write history record", e));
    }

    /**
     * Updates the entrantStatus field in a user's eventHistory record.
     *
     * @param userId The user's Firestore ID.
     * @param eventId The event ID.
     * @param newStatus The new entrant status.
     */
    public static void updateHistoryStatus(String userId, String eventId, String newStatus) {
        if (userId == null || eventId == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("eventHistory").document(eventId)
                .update("entrantStatus", newStatus)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update history status", e));
    }

    /**
     * Deletes a user's eventHistory record (e.g., when leaving the waitlist).
     *
     * @param userId The user's Firestore ID.
     * @param eventId The event ID.
     */
    public static void deleteHistoryRecord(String userId, String eventId) {
        if (userId == null || eventId == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("eventHistory").document(eventId)
                .delete()
                .addOnFailureListener(e -> Log.w(TAG, "Failed to delete history record", e));
    }

    /**
     * Completely deletes an event and all its subcollections (entrants, comments)
     * as well as the poster from Firebase Storage.
     *
     * @param eventId   The event's Firestore document ID.
     * @param onSuccess Called after the batch commit succeeds.
     * @param onFailure Called if the deletion fails.
     */
    public static void deleteEventCompletely(@NonNull String eventId,
                                             @Nullable Runnable onSuccess,
                                             @Nullable java.util.function.Consumer<Exception> onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(eventId);

        // Delete poster from Storage (fire-and-forget)
        FirebaseStorage.getInstance().getReference()
                .child("posters/" + eventId + ".jpg")
                .delete()
                .addOnFailureListener(e -> Log.d(TAG, "No poster to delete for event: " + eventId));

        // Fetch event-related documents, then batch-delete everything.
        eventRef.collection("entrants").get()
                .addOnSuccessListener(entrantSnapshot -> {
                    eventRef.collection("comments").get()
                            .addOnSuccessListener(commentSnapshot -> {
                                fetchNotificationRefsForEvent(db, eventId, notificationRefs ->
                                        fetchNotificationLogRefsForEvent(db, eventId, logRefs -> {
                                            List<DocumentReference> refsToDelete = new ArrayList<>();

                                            for (DocumentSnapshot doc : entrantSnapshot.getDocuments()) {
                                                refsToDelete.add(doc.getReference());
                                            }
                                            for (DocumentSnapshot doc : commentSnapshot.getDocuments()) {
                                                refsToDelete.add(doc.getReference());
                                            }
                                            refsToDelete.addAll(notificationRefs);
                                            refsToDelete.addAll(logRefs);
                                            refsToDelete.add(eventRef);

                                            deleteReferencesInBatches(db, refsToDelete,
                                                    () -> {
                                                        Log.d(TAG, "Completely deleted event: " + eventId);
                                                        if (onSuccess != null) onSuccess.run();
                                                    },
                                                    e -> {
                                                        Log.e(TAG, "Failed to delete event completely", e);
                                                        if (onFailure != null) onFailure.accept(e);
                                                    });
                                        }));
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to fetch comments for deletion", e);
                                if (onFailure != null) onFailure.accept(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch entrants for deletion", e);
                    if (onFailure != null) onFailure.accept(e);
                });
    }

    private static void fetchNotificationRefsForEvent(FirebaseFirestore db,
                                                      String eventId,
                                                      @NonNull java.util.function.Consumer<List<DocumentReference>> onComplete) {
        db.collection("users").get()
                .addOnSuccessListener(userSnapshot -> {
                    List<DocumentReference> notificationRefs = new ArrayList<>();
                    List<QueryDocumentSnapshot> userDocs = new ArrayList<>();
                    for (QueryDocumentSnapshot userDoc : userSnapshot) {
                        userDocs.add(userDoc);
                    }

                    if (userDocs.isEmpty()) {
                        onComplete.accept(notificationRefs);
                        return;
                    }

                    AtomicInteger remainingUsers = new AtomicInteger(userDocs.size());
                    for (QueryDocumentSnapshot userDoc : userDocs) {
                        userDoc.getReference()
                                .collection("notifications")
                                .whereEqualTo("eventId", eventId)
                                .get()
                                .addOnSuccessListener(notificationSnapshot -> {
                                    for (DocumentSnapshot doc : notificationSnapshot.getDocuments()) {
                                        notificationRefs.add(doc.getReference());
                                    }
                                    if (remainingUsers.decrementAndGet() == 0) {
                                        onComplete.accept(notificationRefs);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Failed to fetch notifications for user during event deletion", e);
                                    if (remainingUsers.decrementAndGet() == 0) {
                                        onComplete.accept(notificationRefs);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to fetch users for notification cleanup", e);
                    onComplete.accept(new ArrayList<>());
                });
    }

    private static void fetchNotificationLogRefsForEvent(FirebaseFirestore db,
                                                         String eventId,
                                                         @NonNull java.util.function.Consumer<List<DocumentReference>> onComplete) {
        db.collection("notification_logs")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(logSnapshot -> {
                    List<DocumentReference> logRefs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : logSnapshot) {
                        logRefs.add(doc.getReference());
                    }
                    onComplete.accept(logRefs);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to fetch notification logs for deletion", e);
                    onComplete.accept(new ArrayList<>());
                });
    }

    private static void deleteReferencesInBatches(FirebaseFirestore db,
                                                  List<DocumentReference> refsToDelete,
                                                  @Nullable Runnable onSuccess,
                                                  @Nullable java.util.function.Consumer<Exception> onFailure) {
        if (refsToDelete.isEmpty()) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        int totalBatches = (refsToDelete.size() + MAX_BATCH_DELETES - 1) / MAX_BATCH_DELETES;
        AtomicInteger remainingBatches = new AtomicInteger(totalBatches);

        for (int start = 0; start < refsToDelete.size(); start += MAX_BATCH_DELETES) {
            int end = Math.min(start + MAX_BATCH_DELETES, refsToDelete.size());
            WriteBatch batch = db.batch();

            for (int i = start; i < end; i++) {
                batch.delete(refsToDelete.get(i));
            }

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        if (remainingBatches.decrementAndGet() == 0 && onSuccess != null) {
                            onSuccess.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remainingBatches.getAndSet(-1) > 0 && onFailure != null) {
                            onFailure.accept(e);
                        }
                    });
        }
    }

    /**
     * Completely deletes a user, all their created events (with subcollections),
     * and all user subcollections (notifications, eventHistory).
     *
     * @param userId    The user's Firestore document ID.
     * @param onSuccess Called after all deletions succeed.
     * @param onFailure Called if any deletion fails.
     */
    public static void deleteUserCompletely(@NonNull String userId,
                                            @Nullable Runnable onSuccess,
                                            @Nullable java.util.function.Consumer<Exception> onFailure) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Delete profile picture from Storage (fire-and-forget)
        FirebaseStorage.getInstance().getReference()
                .child("profile_pictures/" + userId + ".jpg")
                .delete()
                .addOnFailureListener(e -> Log.d(TAG, "No profile picture to delete for user: " + userId));

        // Step 1: Delete all events created by this user (with subcollections)
        db.collection("events")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<QueryDocumentSnapshot> eventDocs = new java.util.ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        eventDocs.add(doc);
                    }

                    if (eventDocs.isEmpty()) {
                        deleteUserDocAndSubcollections(db, userId, onSuccess, onFailure);
                        return;
                    }

                    AtomicInteger remaining = new AtomicInteger(eventDocs.size());
                    for (QueryDocumentSnapshot eventDoc : eventDocs) {
                        String eventId = eventDoc.getString("id");
                        if (eventId == null) eventId = eventDoc.getId();

                        deleteEventCompletely(eventId, () -> {
                            if (remaining.decrementAndGet() == 0) {
                                deleteUserDocAndSubcollections(db, userId, onSuccess, onFailure);
                            }
                        }, e -> {
                            Log.e(TAG, "Failed to delete user's event", e);
                            if (remaining.decrementAndGet() == 0) {
                                deleteUserDocAndSubcollections(db, userId, onSuccess, onFailure);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query user's events for deletion", e);
                    if (onFailure != null) onFailure.accept(e);
                });
    }

    /**
     * Deletes a user document and all its subcollections (notifications, eventHistory).
     */
    private static void deleteUserDocAndSubcollections(FirebaseFirestore db, String userId,
                                                        @Nullable Runnable onSuccess,
                                                        @Nullable java.util.function.Consumer<Exception> onFailure) {
        // Fetch notifications and eventHistory subcollections
        db.collection("users").document(userId).collection("notifications").get()
                .addOnSuccessListener(notifSnapshot -> {
                    db.collection("users").document(userId).collection("eventHistory").get()
                            .addOnSuccessListener(historySnapshot -> {
                                WriteBatch batch = db.batch();

                                for (DocumentSnapshot doc : notifSnapshot.getDocuments()) {
                                    batch.delete(doc.getReference());
                                }
                                for (DocumentSnapshot doc : historySnapshot.getDocuments()) {
                                    batch.delete(doc.getReference());
                                }
                                batch.delete(db.collection("users").document(userId));

                                batch.commit()
                                        .addOnSuccessListener(v -> {
                                            Log.d(TAG, "Completely deleted user: " + userId);
                                            if (onSuccess != null) onSuccess.run();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to delete user completely", e);
                                            if (onFailure != null) onFailure.accept(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to fetch eventHistory for deletion", e);
                                if (onFailure != null) onFailure.accept(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch notifications for deletion", e);
                    if (onFailure != null) onFailure.accept(e);
                });
    }
}
