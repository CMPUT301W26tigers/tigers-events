package com.example.eventsapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * A fragment that displays the full details of a specific event.
 * It provides functionality for users to:
 * - View event description, date, and registration period.
 * - See current waitlist statistics (e.g., "5/50 on waitlist").
 * - Join or leave the event's waitlist.
 */
public class EventDetailFragment extends Fragment {

    private static final String TAG = "EventDetailFragment";

    private final FirestoreNotificationHelper notificationHelper = new FirestoreNotificationHelper();
    private String eventId;
    private boolean fromHistory = false;
    private FirebaseFirestore db;

    private TextView tvName, tvDescription, tvEventDate, tvRegistrationStart,
            tvRegistrationEnd, tvCapacity, tvWaitlistCounter, tvExpiredBanner;
    private ImageView ivPoster;
    private MaterialButton btnWaitlist;

    private boolean isOnWaitlist = false;
    private String currentEntrantDocId = null;
    private int waitlistCount = 0;
    private int eventCapacity = 0;

    /**
     * Called when the fragment is being created. Initializes Firestore and
     * retrieves the event ID from the arguments.
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
            fromHistory = getArguments().getBoolean("fromHistory", false);
        } else {
            eventId = "";
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned. Initializes the UI components and sets up listeners for the waitlist button.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tvName = view.findViewById(R.id.tv_name);
        tvDescription = view.findViewById(R.id.tv_description);
        tvEventDate = view.findViewById(R.id.tv_event_date);
        tvRegistrationStart = view.findViewById(R.id.tv_registration_start);
        tvRegistrationEnd = view.findViewById(R.id.tv_registration_end);
        tvCapacity = view.findViewById(R.id.tv_capacity);
        tvWaitlistCounter = view.findViewById(R.id.tv_waitlist_counter);
        tvExpiredBanner = view.findViewById(R.id.tv_expired_banner);
        ivPoster = view.findViewById(R.id.iv_poster);
        btnWaitlist = view.findViewById(R.id.btnWaitlist);

        if (eventId.isEmpty()) {
            tvName.setText("Event not found");
            btnWaitlist.setVisibility(View.GONE);
            return;
        }

        if (fromHistory) {
            // Load from user's personal history instead of global events
            btnWaitlist.setVisibility(View.GONE);
            tvWaitlistCounter.setVisibility(View.GONE);
            if (tvExpiredBanner != null) {
                tvExpiredBanner.setVisibility(View.VISIBLE);
            }
            loadHistoryEventDetails();
        } else {
            loadEventDetails();
            loadWaitlistStatus();

            btnWaitlist.setOnClickListener(v -> {
                if (isOnWaitlist) {
                    leaveWaitlist();
                } else {
                    joinWaitlist();
                }
            });
        }
    }

    /**
     * Fetches the event details from Firestore and updates the UI.
     */
    private void loadEventDetails() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc != null && doc.exists()) {
                        tvName.setText(doc.getString("name"));
                        tvDescription.setText(getFieldOrDefault(doc, "description", "No description available"));
                        tvEventDate.setText(formatDate(doc, "event_date", "TBD"));
                        tvRegistrationStart.setText(formatDate(doc, "registration_start", "TBD"));
                        tvRegistrationEnd.setText(formatDate(doc, "registration_end", "TBD"));

                        Long amountLong = doc.getLong("amount");
                        eventCapacity = (amountLong != null) ? amountLong.intValue() : 0;
                        tvCapacity.setText(String.valueOf(eventCapacity));

                        updateWaitlistCounter();
                    } else {
                        tvName.setText("Event not found");
                        btnWaitlist.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvName.setText("Failed to load event");
                    btnWaitlist.setVisibility(View.GONE);
                });
    }

    /**
     * Fetches the current waitlist status for the event and the current user.
     */
    private void loadWaitlistStatus() {
        // Load total waitlist count
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "APPLIED")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    waitlistCount = querySnapshot.size();
                    updateWaitlistCounter();
                });

        // Check if current user is already on the waitlist
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            db.collection("events").document(eventId)
                    .collection("entrants")
                    .whereEqualTo("userId", currentUser.getId())
                    .whereEqualTo("status", "APPLIED")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!isAdded()) return;
                        if (!querySnapshot.isEmpty()) {
                            isOnWaitlist = true;
                            currentEntrantDocId = querySnapshot.getDocuments().get(0).getId();
                            updateButtonState();
                        }
                    });
        }
    }

    /**
     * Adds the current user to the event's waitlist in Firestore.
     */
    private void joinWaitlist() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to join the waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        btnWaitlist.setEnabled(false);

        String entrantId = java.util.UUID.randomUUID().toString();
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("id", entrantId);
        entrantData.put("eventId", eventId);
        entrantData.put("name", currentUser.getName());
        entrantData.put("email", currentUser.getEmail());
        entrantData.put("status", "APPLIED");
        entrantData.put("userId", currentUser.getId());
        entrantData.put("statusCode", 0);

        db.collection("events").document(eventId)
                .collection("entrants").document(entrantId)
                .set(entrantData)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    isOnWaitlist = true;
                    currentEntrantDocId = entrantId;
                    notificationHelper.sendWaitlistedNotification(currentUser.getId(), eventId);
                    waitlistCount++;
                    updateButtonState();
                    updateWaitlistCounter();
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Joined the waitlist!", Toast.LENGTH_SHORT).show();
                    // Write event history record for the user
                    writeEventHistoryForCurrentUser(currentUser.getId());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error joining waitlist", e);
                });
    }

    /**
     * Removes the current user from the event's waitlist in Firestore.
     */
    private void leaveWaitlist() {
        if (currentEntrantDocId == null) return;

        btnWaitlist.setEnabled(false);

        db.collection("events").document(eventId)
                .collection("entrants").document(currentEntrantDocId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    isOnWaitlist = false;
                    currentEntrantDocId = null;
                    waitlistCount = Math.max(0, waitlistCount - 1);
                    updateButtonState();
                    updateWaitlistCounter();
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Removed from waitlist", Toast.LENGTH_SHORT).show();
                    // Delete history record since user voluntarily left
                    Users user = UserManager.getInstance().getCurrentUser();
                    if (user != null && user.getId() != null) {
                        EventCleanupHelper.deleteHistoryRecord(user.getId(), eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to leave waitlist", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error leaving waitlist", e);
                });
    }

    /**
     * Updates the text and color of the waitlist button based on whether the user is on the waitlist.
     */
    private void updateButtonState() {
        if (isOnWaitlist) {
            btnWaitlist.setText("Leave Waitlist");
            btnWaitlist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.colorDanger, null)));
        } else {
            btnWaitlist.setText("Join Waitlist");
            btnWaitlist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.colorPrimaryPurple, null)));
        }
    }

    /**
     * Updates the waitlist counter text view with current statistics.
     */
    private void updateWaitlistCounter() {
        tvWaitlistCounter.setText(waitlistCount + "/" + eventCapacity + " on waitlist");
    }

    /**
     * Helper method to get a string field from a {@link DocumentSnapshot} or a default value if missing.
     *
     * @param doc The document snapshot.
     * @param field The field name.
     * @param defaultValue The default value to return if the field is null or empty.
     * @return The field value or the default value.
     */
    private String getFieldOrDefault(DocumentSnapshot doc, String field, String defaultValue) {
        String value = doc.getString(field);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Formats a yyyy-MM-dd date string to "Month Day, Year" format.
     * Falls back to the raw string if parsing fails, or defaultValue if empty.
     */
    private String formatDate(DocumentSnapshot doc, String field, String defaultValue) {
        String value = doc.getString(field);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            java.text.SimpleDateFormat input = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CANADA);
            input.setLenient(false);
            java.util.Date date = input.parse(value);
            if (date == null) return value;
            java.text.SimpleDateFormat output = new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.CANADA);
            return output.format(date);
        } catch (java.text.ParseException e) {
            return value;
        }
    }

    /**
     * Fetches the event data from Firestore and writes a copy to the user's eventHistory.
     */
    private void writeEventHistoryForCurrentUser(String userId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("id", eventId);
                    eventData.put("name", doc.getString("name"));
                    Long amount = doc.getLong("amount");
                    eventData.put("amount", amount != null ? amount : 0);
                    eventData.put("description", doc.getString("description"));
                    eventData.put("event_date", doc.getString("event_date"));
                    eventData.put("registration_start", doc.getString("registration_start"));
                    eventData.put("registration_end", doc.getString("registration_end"));
                    eventData.put("posterUrl", doc.getString("posterUrl"));
                    Long sample = doc.getLong("sampleSize");
                    eventData.put("sampleSize", sample != null ? sample : 0);
                    EventCleanupHelper.writeHistoryRecord(userId, eventId, eventData, "APPLIED");
                });
    }

    /**
     * Loads event details from the user's personal eventHistory collection
     * (used when viewing an expired/historical event).
     */
    private void loadHistoryEventDetails() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            tvName.setText("Event not found");
            return;
        }

        db.collection("users").document(currentUser.getId())
                .collection("eventHistory").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc != null && doc.exists()) {
                        tvName.setText(doc.getString("name"));
                        tvDescription.setText(getFieldOrDefault(doc, "description", "No description available"));
                        tvEventDate.setText(formatDate(doc, "event_date", "TBD"));
                        tvRegistrationStart.setText(formatDate(doc, "registration_start", "TBD"));
                        tvRegistrationEnd.setText(formatDate(doc, "registration_end", "TBD"));

                        Long amountLong = doc.getLong("amount");
                        eventCapacity = (amountLong != null) ? amountLong.intValue() : 0;
                        tvCapacity.setText(String.valueOf(eventCapacity));
                    } else {
                        tvName.setText("Event not found");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvName.setText("Failed to load event");
                });
    }
}
