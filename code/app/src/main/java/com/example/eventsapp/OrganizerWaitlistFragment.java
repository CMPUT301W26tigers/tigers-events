package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * A Fragment representing the organizer's view of an event's waitlist.
 * Allows the organizer to view entrants in the pool, and to remove or replace
 * existing entrants. Integrates with Firebase Firestore for real-time updates.
 */
public class OrganizerWaitlistFragment extends Fragment {

    private final FirestoreNotificationHelper notificationHelper = new FirestoreNotificationHelper();
    private FirebaseFirestore db;
    private String eventId;

    private Event currentEvent; // Keeps track of event details (amount, sampleSize)
    private RecyclerView rvWaitlist;
    private TextInputEditText etSearchWaitlist;
    private TextView tvWaitlistStats;
    private OrganizerEntrantAdapter adapter;
    private final List<Entrant> allEntrants = new ArrayList<>();
    private final List<Entrant> filteredEntrants = new ArrayList<>();
    private Set<String> tempSelectedIds = new HashSet<>(); // For drafted list of entrants

    public OrganizerWaitlistFragment() {
        super(R.layout.fragment_organizer_waitlist);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_waitlist);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        rvWaitlist = view.findViewById(R.id.rv_waitlist);
        etSearchWaitlist = view.findViewById(R.id.et_search_waitlist);
        tvWaitlistStats = view.findViewById(R.id.tv_waitlist_stats);

        MaterialButton btnNotifyWaitlisted = view.findViewById(R.id.btn_notify_waitlisted);
        MaterialButton btnNotifySelected = view.findViewById(R.id.btn_notify_selected);
        MaterialButton btnNotifyNotSelected = view.findViewById(R.id.btn_notify_not_selected);
        MaterialButton btnRunLottery = view.findViewById(R.id.btn_run_lottery);

        btnNotifyWaitlisted.setOnClickListener(v -> notifyWaitlistedEntrants());
        btnNotifySelected.setOnClickListener(v -> notifySelectedEntrants());
        btnNotifyNotSelected.setOnClickListener(v -> notifyNotSelectedEntrants());
        btnRunLottery.setOnClickListener(v -> runLottery());

        adapter = new OrganizerEntrantAdapter(filteredEntrants, new OrganizerEntrantAdapter.OnEntrantActionListener() {
            @Override
            public void onRemove(Entrant entrant) {
                removeEntrant(entrant);
            }

            @Override
            public void onReplace(Entrant entrant) {
                replaceEntrant(entrant);
            }
        });

        rvWaitlist.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWaitlist.setAdapter(adapter);

        etSearchWaitlist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntrants(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        loadWaitlistData();
    }

    private void loadWaitlistData() {
        if (eventId == null || eventId.isEmpty()) return;

        // Fetch current event to resolve capacities real-time
        db.collection("events").document(eventId).addSnapshotListener((value, error) -> {
            if (error == null && value != null && value.exists()) {
                currentEvent = value.toObject(Event.class);
                if (currentEvent != null) currentEvent.setId(value.getId());
            }
        });

        db.collection("events").document(eventId)
                .collection("entrants")
                .whereIn("statusCode", Arrays.asList(0, 1, 2))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(requireContext(), "Error loading waitlist", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allEntrants.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Entrant entrant = doc.toObject(Entrant.class);
                            if (entrant != null) {
                                entrant.setId(doc.getId());

                                // Suggested by Gemini on pass through of function, protects against corrupted state
                                if(entrant.getStatus() == null) entrant.setStatus(Entrant.Status.APPLIED);
                                allEntrants.add(entrant);
                            }
                        }
                    }

                    String query = etSearchWaitlist.getText() != null
                            ? etSearchWaitlist.getText().toString()
                            : "";
                    filterEntrants(query);
                });
    }

    private void filterEntrants(@NonNull String query) {
        filteredEntrants.clear();
        String normalizedQuery = query.trim().toLowerCase(Locale.CANADA);

        for (Entrant entrant : allEntrants) {
            String name = entrant.getName() == null ? "" : entrant.getName();
            if (normalizedQuery.isEmpty() || name.toLowerCase(Locale.CANADA).contains(normalizedQuery)) {
                filteredEntrants.add(entrant);
            }
        }

        adapter.notifyDataSetChanged();
        tvWaitlistStats.setText(normalizedQuery.isEmpty()
                ? "Total Entrants: " + allEntrants.size()
                : "Showing " + filteredEntrants.size() + " of " + allEntrants.size());
    }

    private void runLottery() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event data not fully loaded. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic uses sampleSize unless zero, then it falls back to max event capacity
        int capacity = currentEvent.getSampleSize() > 0 ? currentEvent.getSampleSize() : currentEvent.getAmount();

        int occupied = 0;
        List<Entrant> candidates = new ArrayList<>();

        // Group status
        for (Entrant e : allEntrants) {
            if (e.getStatus() == Entrant.Status.INVITED ||
                    e.getStatus() == Entrant.Status.ACCEPTED ||
                    e.getStatus() == Entrant.Status.PRIVATE_INVITED) {
                occupied++;
            } else if (e.getStatus() == Entrant.Status.APPLIED) {
                candidates.add(e);
            }
        }

        int available = capacity - occupied;

        if (available <= 0) {
            Toast.makeText(requireContext(), "No more event space to run a lottery.", Toast.LENGTH_LONG).show();
            return;
        }

        if (candidates.isEmpty()) {
            Toast.makeText(requireContext(), "No uninvited waitlisted entrants available to sample.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Wipe previous lottery drafts locally if they click again
        tempSelectedIds.clear();

        // Perform equally likely sample logic
        Collections.shuffle(candidates);
        int draftCount = Math.min(available, candidates.size());

        for (int i = 0; i < draftCount; i++) {
            tempSelectedIds.add(candidates.get(i).getId());
        }

        adapter.setTempSelectedIds(tempSelectedIds);
        Toast.makeText(requireContext(), "Lottery pulled " + draftCount + " entrant(s)! Click 'Selected' to notify.", Toast.LENGTH_LONG).show();
    }

    private void removeEntrant(Entrant entrantToRemove) {
        if (eventId == null || eventId.isEmpty() || entrantToRemove.getId() == null) return;

        db.collection("events").document(eventId)
                .collection("entrants").document(entrantToRemove.getId())
                .update("status", "CANCELLED", "statusCode", 3)
                .addOnSuccessListener(aVoid -> {
                    notificationHelper.sendNotSelectedNotification(entrantToRemove.getUserId(), eventId);
                    EventCleanupHelper.updateHistoryStatus(entrantToRemove.getUserId(), eventId, "CANCELLED");
                    Toast.makeText(requireContext(), "Entrant removed.", Toast.LENGTH_SHORT).show();
                });
    }

    private void replaceEntrant(Entrant entrantToReplace) {
        if (eventId == null || eventId.isEmpty() || entrantToReplace.getId() == null) return;

        db.collection("events").document(eventId)
                .collection("entrants").document(entrantToReplace.getId())
                .update("status", "CANCELLED", "statusCode", 3)
                .addOnSuccessListener(aVoid -> {
                    notificationHelper.sendNotSelectedNotification(entrantToReplace.getUserId(), eventId);
                    EventCleanupHelper.updateHistoryStatus(entrantToReplace.getUserId(), eventId, "CANCELLED");
                    drawReplacementApplicant();
                });
    }

    private void drawReplacementApplicant() {
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("statusCode", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No more applicants left in the pool.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> applicants = queryDocumentSnapshots.getDocuments();
                    DocumentSnapshot selectedApplicant = applicants.get(new Random().nextInt(applicants.size()));

                    db.collection("events").document(eventId)
                            .collection("entrants").document(selectedApplicant.getId())
                            .update("status", "INVITED", "statusCode", 1)
                            .addOnSuccessListener(aVoid -> {
                                String replacementUserId = selectedApplicant.getString("userId");
                                notificationHelper.sendInvitationNotification(replacementUserId, eventId);
                                EventCleanupHelper.updateHistoryStatus(replacementUserId, eventId, "INVITED");
                                Toast.makeText(requireContext(), "Replacement drawn successfully!", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private interface NotificationAction {
        void send(String userId);
    }

    private void notifyEntrantsByStatusCode(
            int statusCode,
            String emptyMessage,
            String failureMessage,
            NotificationAction action
    ) {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "No event selected", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .whereEqualTo("statusCode", statusCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(requireContext(), emptyMessage, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int sentCount = 0;
                    int skippedCount = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.trim().isEmpty()) {
                            skippedCount++;
                            continue;
                        }

                        action.send(userId);
                        sentCount++;
                    }

                    String message;
                    if (sentCount == 0) {
                        message = "Entrants found, but no linked users could be notified";
                    } else if (skippedCount == 0) {
                        message = "Notifications sent to " + sentCount + " entrants";
                    } else {
                        message = "Notifications sent to " + sentCount
                                + " entrants, skipped " + skippedCount;
                    }

                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), failureMessage, Toast.LENGTH_SHORT).show()
                );
    }

    private void notifyWaitlistedEntrants() {
        notifyEntrantsByStatusCode(
                0,
                "No waitlisted entrants to notify",
                "Failed to notify waitlisted entrants",
                userId -> notificationHelper.sendWaitlistedNotification(userId, eventId)
        );
    }
    private void notifySelectedEntrants() {
        if (!tempSelectedIds.isEmpty()) {
            if (currentEvent == null) return;
            LotteryNotificationController notificationController = new LotteryNotificationController();

            for (String entrantId : tempSelectedIds) {
                String userId = null;
                // Grab the user ID
                for (Entrant e : allEntrants) {
                    if (e.getId().equals(entrantId)) {
                        userId = e.getUserId();
                        break;
                    }
                }

                // Update both Enum String & statusCode for legacy compatibility
                db.collection("events").document(eventId)
                        .collection("entrants").document(entrantId)
                        .update("status", Entrant.Status.INVITED.name(), "statusCode", 1);

                // Use the required Lottery Notification Controller to mutate User doc directly
                if (userId != null && !userId.isEmpty()) {
                    db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Users user = doc.toObject(Users.class);
                            if (user != null) {
                                notificationController.notifyChosenFromWaitlist(user, currentEvent.getName());
                                // Overwrite updated user doc resolving lottery invitation
                                db.collection("users").document(doc.getId()).set(user);
                            }
                        }
                    });
                }
            }

            Toast.makeText(requireContext(), "Lottery finalized! Sent out invitations.", Toast.LENGTH_SHORT).show();
            tempSelectedIds.clear();
            adapter.setTempSelectedIds(tempSelectedIds);
        } else {
            // Re-pinging originally invited folks (fallback if nothing currently drafted)
            notifyEntrantsByStatusCode(
                    1,
                    "No selected entrants to notify",
                    "Failed to notify selected entrants",
                    userId -> notificationHelper.sendInvitationNotification(userId, eventId)
            );
        }
    }
    private void notifyNotSelectedEntrants() {
        notifyEntrantsByStatusCode(
                3,
                "No non-selected entrants to notify",
                "Failed to notify non-selected entrants",
                userId -> notificationHelper.sendNotSelectedNotification(userId, eventId)
        );
    }
}
