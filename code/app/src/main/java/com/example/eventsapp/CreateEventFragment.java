package com.example.eventsapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * A fragment that allows users to create a new event.
 * Handles user stories for event creation (US 02.01.01) including:
 * - Setting event description and poster.
 * - Generating a unique promotional QR code for the event.
 * - Specifying event capacity and lottery sample size.
 */
public class CreateEventFragment extends Fragment {

    private static final int QR_SIZE = 400;

    private TextInputEditText editName;
    private TextInputEditText editDescription;
    private TextInputEditText editCapacity;
    private TextInputEditText editSampleSize;
    private ImageView ivPoster;
    private ImageView ivQR;
    private FirebaseFirestore db;

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_event, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned. Initializes the UI components, generates a random UUID for the new event,
     * and sets up listeners for saving and sharing.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        editName = view.findViewById(R.id.edit_name);
        editDescription = view.findViewById(R.id.edit_description);
        editCapacity = view.findViewById(R.id.edit_capacity);
        editSampleSize = view.findViewById(R.id.edit_sample_size);
        ivPoster = view.findViewById(R.id.iv_poster);
        ivQR = view.findViewById(R.id.iv_qr);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        MaterialButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Create new event - generate unique ID and QR
        Event event = new Event(null, "", 1, "", "", 0);
        event.setId(java.util.UUID.randomUUID().toString());
        updateQRCode(event);

        // Save event
        view.findViewById(R.id.btn_view_waitlist).setOnClickListener(v -> saveAndNavigateToWaitlist(event));

        // Share QR
        view.findViewById(R.id.btn_share_qr).setOnClickListener(v -> {
            if (event.getId() != null) {
                Toast.makeText(requireContext(), "Share QR: " + event.getEventDeepLink(), Toast.LENGTH_SHORT).show();
                // Could integrate with ShareCompat for actual sharing
            }
        });
    }

    /**
     * Generates a QR code bitmap for the event's deep link and updates the ImageView.
     *
     * @param event The event object whose deep link will be encoded.
     */
    private void updateQRCode(Event event) {
        String link = event.getEventDeepLink();
        Bitmap qrBitmap = QRCodeUtil.generateQRCode(link, QR_SIZE, QR_SIZE);
        if (qrBitmap != null && ivQR != null) {
            ivQR.setImageBitmap(qrBitmap);
        }
    }

    /**
     * Validates input fields, updates the {@link Event} object, saves the event to Firestore,
     * and navigates to the waitlist management screen upon success.
     *
     * @param event The event object being created and saved.
     */
    private void saveAndNavigateToWaitlist(Event event) {
        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
        String capacityStr = editCapacity.getText() != null ? editCapacity.getText().toString().trim() : "1";
        String sampleStr = editSampleSize != null && editSampleSize.getText() != null
                ? editSampleSize.getText().toString().trim() : "0";
        int capacity;
        int sampleSize;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                Toast.makeText(requireContext(), "Capacity must be positive", Toast.LENGTH_SHORT).show();
                return;
            }
            sampleSize = Integer.parseInt(sampleStr);
            if (sampleSize < 0) sampleSize = 0;
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid capacity", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Event name required", Toast.LENGTH_SHORT).show();
            return;
        }

        event.setName(name);
        event.setDescription(description);
        event.setAmount(capacity);
        event.setSampleSize(sampleSize);
        updateQRCode(event);

        Map<String, Object> data = new HashMap<>();
        data.put("id", event.getId());
        data.put("name", event.getName());
        data.put("amount", event.getAmount());
        data.put("description", event.getDescription());
        data.put("posterUrl", event.getPosterUrl());
        data.put("sampleSize", event.getSampleSize());

        // Store who created this event for filtering on the Events page
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            data.put("createdBy", currentUser.getId());
        }

        DocumentReference docRef = db.collection("events").document(event.getId());
        docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CreateEvent", "Event saved");
                    Toast.makeText(requireContext(), "Event created", Toast.LENGTH_SHORT).show();
                    Bundle args = new Bundle();
                    args.putString("eventId", event.getId());
                    args.putString("eventName", event.getName());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.viewEntrantsFragment, args);
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateEvent", "Failed to save", e);
                    Toast.makeText(requireContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
                });
    }
}
