package com.example.eventsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class WaitlistFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";

    private MaterialToolbar toolbarWaitlist;
    private MaterialButton btnExportCsv;
    private MaterialButton btnSeeCancelled;
    private FirebaseFirestore db;
    private String eventId;

    public WaitlistFragment() {
        super(R.layout.view_waitlist);
    }

    public static WaitlistFragment newInstance(String eventId) {
        WaitlistFragment fragment = new WaitlistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID, "");
        } else {
            eventId = "";
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        toolbarWaitlist = view.findViewById(R.id.toolbar_waitlist);
        btnExportCsv = view.findViewById(R.id.btn_export_csv);
        btnSeeCancelled = view.findViewById(R.id.btn_see_cancelled);

        setupToolbar();
        setupButtons();
    }

    private void setupToolbar() {
        toolbarWaitlist.inflateMenu(R.menu.waitlist_menu);

        toolbarWaitlist.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        toolbarWaitlist.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_notify) {
                notifyChosenEntrants();
                return true;
            }
            return false;
        });
    }

    private void setupButtons() {
        btnExportCsv.setOnClickListener(v -> exportEntrantsAsCsv());
        btnSeeCancelled.setOnClickListener(v -> openCancelledFragment());
    }

    private void notifyChosenEntrants() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "No event selected", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .whereIn("status", Arrays.asList("INVITED", "ACCEPTED"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(requireContext(), "No chosen entrants to notify", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final int[] sentCount = {0};
                    final int[] skippedCount = {0};

                    for (var doc : querySnapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        if (userId == null || userId.trim().isEmpty()) {
                            skippedCount[0]++;
                            continue;
                        }

                        NotificationItem notification = new NotificationItem(
                                "You've been selected!",
                                "You were chosen to sign up for this event.",
                                eventId,
                                "invitation",
                                false
                        );

                        db.collection("users")
                                .document(userId)
                                .collection("notifications")
                                .add(notification);
                        sentCount[0]++;
                    }

                    String message;
                    if (sentCount[0] == 0) {
                        message = "Chosen entrants found, but no linked users could be notified";
                    } else if (skippedCount[0] == 0) {
                        message = "Notifications sent to " + sentCount[0] + " entrants";
                    } else {
                        message = "Notifications sent to " + sentCount[0]
                                + " entrants, skipped " + skippedCount[0];
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        "Failed to notify chosen entrants",
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void exportEntrantsAsCsv() {
        Toast.makeText(requireContext(), "Export CSV clicked", Toast.LENGTH_SHORT).show();
    }

    private void openCancelledFragment() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, new CancelledFragment())
                .addToBackStack(null)
                .commit();
    }
}
