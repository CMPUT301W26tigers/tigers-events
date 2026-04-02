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
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A Fragment representing the organizer's view of an event's waitlist.
 * Allows the organizer to view entrants in the pool, and to remove or replace
 * existing entrants. Integrates with Firebase Firestore for real-time updates.
 */
public class OrganizerWaitlistFragment extends Fragment {

    private final FirestoreNotificationHelper notificationHelper = new FirestoreNotificationHelper();
    private FirebaseFirestore db;
    private String eventId;
    private RecyclerView rvWaitlist;
    private TextInputEditText etSearchWaitlist;
    private TextView tvWaitlistStats;
    private OrganizerEntrantAdapter adapter;
    private final List<Entrant> allEntrants = new ArrayList<>();
    private final List<Entrant> filteredEntrants = new ArrayList<>();

    public OrganizerWaitlistFragment() {
        super(R.layout.fragment_organizer_waitlist);
    }

    public static OrganizerWaitlistFragment newInstance(String eventId) {
        OrganizerWaitlistFragment fragment = new OrganizerWaitlistFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
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

        btnNotifyWaitlisted.setOnClickListener(v -> notifyWaitlistedEntrants());
        btnNotifySelected.setOnClickListener(v -> notifySelectedEntrants());
        btnNotifyNotSelected.setOnClickListener(v -> notifyNotSelectedEntrants());

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
        notifyEntrantsByStatusCode(
                1,
                "No selected entrants to notify",
                "Failed to notify selected entrants",
                userId -> notificationHelper.sendInvitationNotification(userId, eventId)
        );
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
