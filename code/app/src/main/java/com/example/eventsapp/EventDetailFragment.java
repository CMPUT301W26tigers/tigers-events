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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Shows full event details including waitlist counter and join/leave waitlist button.
 */
public class EventDetailFragment extends Fragment {

    private static final String TAG = "EventDetailFragment";

    private String eventId;
    private FirebaseFirestore db;

    private TextView tvName, tvDescription, tvEventDate, tvRegistrationStart,
            tvRegistrationEnd, tvCapacity, tvWaitlistCounter;
    private ImageView ivPoster;
    private MaterialButton btnWaitlist;

    private boolean isOnWaitlist = false;
    private String currentEntrantDocId = null;
    private int waitlistCount = 0;
    private int eventCapacity = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
        } else {
            eventId = "";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

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
        ivPoster = view.findViewById(R.id.iv_poster);
        btnWaitlist = view.findViewById(R.id.btnWaitlist);

        if (eventId.isEmpty()) {
            tvName.setText("Event not found");
            btnWaitlist.setVisibility(View.GONE);
            return;
        }

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

    private void loadEventDetails() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc != null && doc.exists()) {
                        tvName.setText(doc.getString("name"));
                        tvDescription.setText(getFieldOrDefault(doc, "description", "No description available"));
                        tvEventDate.setText(getFieldOrDefault(doc, "event_date", "TBD"));
                        tvRegistrationStart.setText(getFieldOrDefault(doc, "registration_start", "TBD"));
                        tvRegistrationEnd.setText(getFieldOrDefault(doc, "registration_end", "TBD"));

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
        entrantData.put("name", currentUser.getFullName());
        entrantData.put("email", currentUser.getEmail());
        entrantData.put("status", "APPLIED");
        entrantData.put("userId", currentUser.getId());

        db.collection("events").document(eventId)
                .collection("entrants").document(entrantId)
                .set(entrantData)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    isOnWaitlist = true;
                    currentEntrantDocId = entrantId;
                    waitlistCount++;
                    updateButtonState();
                    updateWaitlistCounter();
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Joined the waitlist!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error joining waitlist", e);
                });
    }

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
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to leave waitlist", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error leaving waitlist", e);
                });
    }

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

    private void updateWaitlistCounter() {
        tvWaitlistCounter.setText(waitlistCount + "/" + eventCapacity + " on waitlist");
    }

    private String getFieldOrDefault(DocumentSnapshot doc, String field, String defaultValue) {
        String value = doc.getString(field);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
