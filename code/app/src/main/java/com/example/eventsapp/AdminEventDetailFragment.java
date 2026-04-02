package com.example.eventsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class AdminEventDetailFragment extends Fragment {

    private static final String ARG_EVENT = "event";

    public AdminEventDetailFragment() {
        super(R.layout.fragment_admin_event_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnAdminEventBack).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack()
        );

        Bundle args = getArguments();
        if (args == null) return;

        Event event = BundleCompat.getSerializable(args, ARG_EVENT, Event.class);
        if (event == null) return;

        ((TextView) view.findViewById(R.id.tvAdminEventName)).setText(event.getName());
        ((TextView) view.findViewById(R.id.tvAdminEventDate)).setText(event.getFormattedEventDate());

        String regWindow = event.getRegistration_start() + " → " + event.getRegistration_end();
        ((TextView) view.findViewById(R.id.tvAdminEventRegWindow)).setText(regWindow);

        ((TextView) view.findViewById(R.id.tvAdminEventCapacity)).setText(String.valueOf(event.getAmount()));
        ((TextView) view.findViewById(R.id.tvAdminEventSampleSize)).setText(String.valueOf(event.getSampleSize()));

        String desc = event.getDescription();
        ((TextView) view.findViewById(R.id.tvAdminEventDescription)).setText(
                (desc != null && !desc.isEmpty()) ? desc : "No description provided."
        );

        // Fetch host name from Firestore using hostId
        TextView tvHost = view.findViewById(R.id.tvAdminEventHost);
        String hostId = event.getHostId();
        if (hostId != null && !hostId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(hostId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name == null || name.isEmpty()) {
                                String first = doc.getString("firstName") != null ? doc.getString("firstName") : "";
                                String last = doc.getString("lastName") != null ? doc.getString("lastName") : "";
                                name = (first + " " + last).trim();
                            }
                            tvHost.setText("Hosted by: " + (name.isEmpty() ? "Unknown" : name));
                        } else {
                            tvHost.setText("Hosted by: Unknown");
                        }
                    })
                    .addOnFailureListener(e -> tvHost.setText("Hosted by: Unknown"));
        } else {
            tvHost.setText("Hosted by: Unknown");
        }

        // Display poster if available
        ImageView ivPoster = view.findViewById(R.id.ivAdminEventPoster);
        View posterPlaceholder = view.findViewById(R.id.posterPlaceholder);
        MaterialButton btnDeletePoster = view.findViewById(R.id.btnDeletePoster);
        String posterUrl = event.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this).load(posterUrl).into(ivPoster);
            ivPoster.setVisibility(View.VISIBLE);
            posterPlaceholder.setVisibility(View.GONE);
            btnDeletePoster.setVisibility(View.VISIBLE);
        }
        btnDeletePoster.setOnClickListener(v -> showDeletePosterConfirmation(event, btnDeletePoster, ivPoster, posterPlaceholder));

        // Delete event button
        view.findViewById(R.id.btnDeleteEvent).setOnClickListener(v ->
                showDeleteEventConfirmation(view, event)
        );
    }

    private void showDeletePosterConfirmation(Event event, MaterialButton btnDeletePoster,
                                               ImageView ivPoster, View posterPlaceholder) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Poster")
                .setMessage("Are you sure you want to delete the poster for \"" + event.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deletePoster(event, btnDeletePoster, ivPoster, posterPlaceholder))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePoster(Event event, MaterialButton btnDeletePoster,
                              ImageView ivPoster, View posterPlaceholder) {
        // Delete from Firebase Storage
        FirebaseStorage.getInstance().getReference()
                .child("posters/" + event.getId() + ".jpg")
                .delete();

        // Clear the URL in Firestore
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(event.getId())
                .update("posterUrl", "")
                .addOnSuccessListener(aVoid -> {
                    event.setPosterUrl("");
                    btnDeletePoster.setVisibility(View.GONE);
                    ivPoster.setVisibility(View.GONE);
                    posterPlaceholder.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Poster deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to delete poster", Toast.LENGTH_SHORT).show()
                );
    }

    private void showDeleteEventConfirmation(View view, Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + event.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(view, event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent(View view, Event event) {
        EventCleanupHelper.deleteEventCompletely(event.getId(),
                () -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                },
                e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
                }
        );
    }
}
