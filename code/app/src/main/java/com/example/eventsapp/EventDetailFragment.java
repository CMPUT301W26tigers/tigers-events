package com.example.eventsapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
    private FirebaseFirestore db;

    private TextView tvName, tvDescription, tvEventDate, tvRegistrationStart,
            tvRegistrationEnd, tvCapacity, tvWaitlistCounter;
    private ImageView ivPoster;
    private MaterialButton btnWaitlist;

    private boolean isOnWaitlist = false;
    private String currentEntrantDocId = null;
    private int waitlistCount = 0;
    private int eventCapacity = 0;
    private boolean geolocationRequired = false;

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    joinWaitlistWithLocation();
                } else {
                    Toast.makeText(requireContext(),
                            "Location permission is required to join this event's waitlist",
                            Toast.LENGTH_LONG).show();
                }
            });

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
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
                        tvEventDate.setText(getFieldOrDefault(doc, "event_date", "TBD"));
                        tvRegistrationStart.setText(getFieldOrDefault(doc, "registration_start", "TBD"));
                        tvRegistrationEnd.setText(getFieldOrDefault(doc, "registration_end", "TBD"));

                        Long amountLong = doc.getLong("amount");
                        eventCapacity = (amountLong != null) ? amountLong.intValue() : 0;
                        tvCapacity.setText(String.valueOf(eventCapacity));

                        // US 02.02.03: read geolocation requirement set by organizer
                        Boolean geoReq = doc.getBoolean("geolocationRequired");
                        geolocationRequired = (geoReq != null) && geoReq;

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
     * Entry point when the user taps "Join Waitlist".
     * If geolocation is required (US 02.02.03), requests location permission first.
     * Otherwise joins immediately without location.
     */
    private void joinWaitlist() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to join the waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (geolocationRequired) {
            // US 02.02.03: location is required — explain to user then request permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                joinWaitlistWithLocation();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Location Required")
                        .setMessage("This event requires you to share your location when joining the waitlist so the organiser can see where entrants joined from. Your location will be recorded once.")
                        .setPositiveButton("Allow", (d, w) ->
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        } else {
            joinWaitlistWithData(0.0, 0.0);
        }
    }

    /**
     * Gets the current device location then joins the waitlist with that position.
     * Used when the organiser has enabled geolocation (US 02.02.02 / US 02.02.03).
     */
    @SuppressLint("MissingPermission")
    private void joinWaitlistWithLocation() {
        btnWaitlist.setEnabled(false);
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) return;
                    if (location != null) {
                        joinWaitlistWithData(location.getLatitude(), location.getLongitude());
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.getLastLocation().addOnSuccessListener(last -> {
                            if (!isAdded()) return;
                            if (last != null) {
                                joinWaitlistWithData(last.getLatitude(), last.getLongitude());
                            } else {
                                Toast.makeText(requireContext(),
                                        "Could not get location. Please enable GPS and try again.",
                                        Toast.LENGTH_LONG).show();
                                btnWaitlist.setEnabled(true);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Writes the entrant document to Firestore, including lat/lng if provided.
     * Implements US 02.02.02: stores where the entrant joined from.
     *
     * @param latitude  GPS latitude (0.0 if geolocation not required).
     * @param longitude GPS longitude (0.0 if geolocation not required).
     */
    private void joinWaitlistWithData(double latitude, double longitude) {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

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
        entrantData.put("latitude", latitude);
        entrantData.put("longitude", longitude);

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
}
