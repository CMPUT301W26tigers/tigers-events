package com.example.eventsapp;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class that handles automatic cleanup of expired events.
 * Events are considered expired 12 hours after their event_date.
 * Before deletion, all participants' eventHistory records are marked as expired.
 */
public class EventCleanupHelper {

    private static final String TAG = "EventCleanupHelper";
    private static final int EXPIRATION_HOURS = 12;

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
                            markHistoryAndDelete(db, eventId, createdBy);
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
    private static void markHistoryAndDelete(FirebaseFirestore db, String eventId, String createdBy) {
        // Mark organizer's history as expired
        if (createdBy != null && !createdBy.isEmpty()) {
            db.collection("users").document(createdBy)
                    .collection("eventHistory").document(eventId)
                    .update("expired", true)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to mark organizer history expired", e));
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
}
