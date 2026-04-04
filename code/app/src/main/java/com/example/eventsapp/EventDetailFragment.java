package com.example.eventsapp;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import android.location.Location;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment that displays the full details of a specific event.
 * It provides functionality for users to:
 * - View event description, date, and registration period.
 * - See current waitlist statistics (e.g., "5/50 on waitlist").
 * - Join or leave the event's waitlist.
 * - View and post comments on the event.
 */
public class EventDetailFragment extends Fragment {

    private static final String TAG = "EventDetailFragment";

    private final FirestoreNotificationHelper notificationHelper = new FirestoreNotificationHelper();
    private String eventId;
    private boolean fromHistory = false;
    private FirebaseFirestore db;

    private TextView tvName, tvDescription, tvEventDate, tvRegistrationRange,
            tvCapacity, tvWaitlistCounter, tvExpiredBanner, tvHostName, tvHostAvatar, tvLocation;
    private ImageView ivPoster, ivHostPicture;
    private MaterialButton btnWaitlist;

    // Comment views
    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;
    private TextInputEditText etComment;
    private MaterialButton btnPostComment;

    private boolean isOnWaitlist = false;
    private boolean isPrivateEvent = false;
    private boolean userIsOrganizer = false;
    private String currentEntrantDocId = null;
    private int waitlistCount = 0;
    private int waitlistCapacity = 0;
    private int eventCapacity = 0;
    private String eventCreatorId = null;
    private boolean geolocationRequired = false;
    /** False until {@link #loadEventDetails()} finishes; avoids joining before we know if location is required. */
    private boolean eventDetailsLoaded = false;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestLocationPermission;

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
        if (isGooglePlayServicesLocationAvailable()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        }
        requestLocationPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isAdded()) return;
                    Users u = UserManager.getInstance().getCurrentUser();
                    if (u == null || u.getId() == null) {
                        btnWaitlist.setEnabled(true);
                        return;
                    }
                    if (geolocationRequired) {
                        if (granted) {
                            fetchLocationAndCompleteJoin(true);
                        } else {
                            btnWaitlist.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    "Location permission is required to join this event",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        if (granted) {
                            fetchLocationAndCompleteJoin(false);
                        } else {
                            writeEntrantToFirestore(u, 0.0, 0.0, null);
                        }
                    }
                });
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
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        tvName = view.findViewById(R.id.tv_name);
        tvDescription = view.findViewById(R.id.tv_description);
        tvEventDate = view.findViewById(R.id.tv_event_date);
        tvRegistrationRange = view.findViewById(R.id.tv_registration_range);
        tvCapacity = view.findViewById(R.id.tv_capacity);
        tvWaitlistCounter = view.findViewById(R.id.tv_waitlist_counter);
        tvExpiredBanner = view.findViewById(R.id.tv_expired_banner);
        tvHostName = view.findViewById(R.id.tv_host_name);
        tvHostAvatar = view.findViewById(R.id.tv_host_avatar);
        tvLocation = view.findViewById(R.id.tv_location);
        ivPoster = view.findViewById(R.id.iv_poster);
        ivHostPicture = view.findViewById(R.id.iv_host_picture);
        btnWaitlist = view.findViewById(R.id.btnWaitlist);

        view.findViewById(R.id.btnInfo).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("How joining events work:")
                    .setMessage("filler description")
                    .setPositiveButton("OK", null)
                    .show();
        });

        // Initialize comments section
        rvComments = view.findViewById(R.id.rv_comments);
        etComment = view.findViewById(R.id.et_comment);
        btnPostComment = view.findViewById(R.id.btn_post_comment);

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);

        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            commentAdapter.setCurrentUserId(currentUser.getId());
            commentAdapter.setCurrentUserAccountType(currentUser.getAccountType());
        }

        commentAdapter.setOnCommentDeleteListener(this::deleteComment);

        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComments.setAdapter(commentAdapter);

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

        loadComments();
        btnPostComment.setOnClickListener(v -> postComment());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!fromHistory && !eventId.isEmpty()) {
            startWaitlistLocationSharingIfNeeded();
        }
    }

    /**
     * Near–real-time location for the organizer map: runs in a foreground service so updates
     * continue when the user leaves this screen (not only while it is visible).
     */
    private void startWaitlistLocationSharingIfNeeded() {
        if (fromHistory || !isOnWaitlist || currentEntrantDocId == null || eventId.isEmpty()) {
            return;
        }
        if (!hasLocationPermission()) {
            return;
        }
        String displayName = "";
        if (tvName != null && tvName.getText() != null) {
            displayName = tvName.getText().toString().trim();
        }
        WaitlistLocationForegroundService.start(requireContext(), eventId, currentEntrantDocId, displayName);
    }

    private void stopWaitlistLocationSharing() {
        WaitlistLocationForegroundService.stop(requireContext());
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
                        tvRegistrationRange.setText(
                                formatDate(doc, "registration_start", "TBD")
                                + " - "
                                + formatDate(doc, "registration_end", "TBD"));
                        isPrivateEvent = Boolean.TRUE.equals(doc.getBoolean("isPrivate"));
                        userIsOrganizer = isCurrentUserOrganizer(doc);
                        eventCreatorId = doc.getString("createdBy");
                        if (commentAdapter != null) {
                            commentAdapter.setEventCreatorId(eventCreatorId);
                        }

                        Long amountLong = doc.getLong("amount");
                        eventCapacity = (amountLong != null) ? amountLong.intValue() : 0;
                        tvCapacity.setText(String.valueOf(eventCapacity));

                        Long waitlistCapLong = doc.getLong("waitlistCapacity");
                        waitlistCapacity = (waitlistCapLong != null) ? waitlistCapLong.intValue() : 0;

                        String posterUrl = doc.getString("posterUrl");
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this).load(posterUrl).into(ivPoster);
                        } else {
                            ivPoster.setImageResource(android.R.color.darker_gray);
                        }

                        // Location
                        String location = doc.getString("location");
                        if (location != null && !location.isEmpty()) {
                            tvLocation.setText(location);
                        } else {
                            tvLocation.setText("No location set");
                        }

                        // Resolve host name
                        resolveHostName(eventCreatorId);

                        applyGeolocationRequiredFromEvent(doc);
                        eventDetailsLoaded = true;

                        updateWaitlistCounter();
                        refreshWaitlistButtonState();
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
                            refreshWaitlistButtonState();
                            startWaitlistLocationSharingIfNeeded();
                        }
                    });
        }
    }

    /**
     * Adds the current user to the event's waitlist in Firestore.
     */
    private void joinWaitlist() {
        if (userIsOrganizer || isPrivateEvent) {
            return;
        }

        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to join the waitlist", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.getId() == null || currentUser.getId().isEmpty()) {
            Toast.makeText(requireContext(), "Please sign in to join the waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (waitlistCapacity > 0 && waitlistCount >= waitlistCapacity && !isOnWaitlist) {
            Toast.makeText(requireContext(), "The waitlist for this event is full", Toast.LENGTH_SHORT).show();
            return;
        }
        btnWaitlist.setEnabled(false);

        if (!eventDetailsLoaded) {
            fetchEventSettingsThenContinueJoin(currentUser);
            return;
        }
        continueJoinWithResolvedGeoSetting(currentUser);
    }

    /**
     * Re-reads the event document so "require location" is known even if the user taps Join
     * before {@link #loadEventDetails()} completes (slow network).
     */
    private void fetchEventSettingsThenContinueJoin(@NonNull Users currentUser) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc == null || !doc.exists()) {
                        btnWaitlist.setEnabled(true);
                        Toast.makeText(requireContext(), "Could not load event", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    applyGeolocationRequiredFromEvent(doc);
                    eventDetailsLoaded = true;
                    continueJoinWithResolvedGeoSetting(currentUser);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Could not load event", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "join: failed to load event settings", e);
                });
    }

    /**
     * Reads organizer "require location" flag from Firestore (boolean, or legacy numeric / string).
     */
    private void applyGeolocationRequiredFromEvent(@NonNull DocumentSnapshot doc) {
        Boolean geo = doc.getBoolean("geolocationRequired");
        if (geo != null) {
            geolocationRequired = geo;
            return;
        }
        Long asLong = doc.getLong("geolocationRequired");
        if (asLong != null) {
            geolocationRequired = asLong != 0L;
            return;
        }
        String s = doc.getString("geolocationRequired");
        if (s != null) {
            geolocationRequired = "true".equalsIgnoreCase(s.trim()) || "1".equals(s.trim());
            return;
        }
        geolocationRequired = false;
    }

    private void continueJoinWithResolvedGeoSetting(@NonNull Users currentUser) {
        if (geolocationRequired) {
            if (!hasLocationPermission()) {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
            fetchLocationAndCompleteJoin(true);
        } else {
            if (hasLocationPermission()) {
                fetchLocationAndCompleteJoin(false);
            } else {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests a FRESH GPS fix (not a stale cached location) before writing the entrant to Firestore.
     *
     * Strategy:
     *  1. Start a high-accuracy location update.
     *  2. Use the first result that arrives (real GPS fix).
     *  3. If the fix takes longer than 10 s, fall back to getLastLocation.
     *  4. If there is no last location and the join is required, block the join.
     */
    private void fetchLocationAndCompleteJoin(boolean required) {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            btnWaitlist.setEnabled(true);
            return;
        }

        if (!isAdded()) return;
        if (fusedLocationClient == null || !isGooglePlayServicesLocationAvailable()) {
            Log.w(TAG, "Fused location unavailable; continuing without Play services location");
            onNoLocation(required, currentUser);
            return;
        }
        Toast.makeText(requireContext(), "Getting your location…", Toast.LENGTH_SHORT).show();

        LocationRequest freshRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                .setMinUpdateIntervalMillis(1_000L)
                .setMaxUpdates(1)
                .build();

        LocationCallback[] callbackHolder = new LocationCallback[1];
        android.os.Handler timeoutHandler = new android.os.Handler(Looper.getMainLooper());

        callbackHolder[0] = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                timeoutHandler.removeCallbacksAndMessages(null);
                fusedLocationClient.removeLocationUpdates(callbackHolder[0]);
                if (!isAdded()) return;
                Location loc = result.getLastLocation();
                if (loc == null) {
                    onNoLocation(required, currentUser);
                    return;
                }
                Float accuracy = loc.hasAccuracy() ? loc.getAccuracy() : null;
                writeEntrantToFirestore(currentUser, loc.getLatitude(), loc.getLongitude(), accuracy);
            }
        };

        // Timeout: fall back to getLastLocation after 10 s
        timeoutHandler.postDelayed(() -> {
            if (!isAdded()) return;
            fusedLocationClient.removeLocationUpdates(callbackHolder[0]);
            if (!hasLocationPermission()) {
                onNoLocation(required, currentUser);
                return;
            }
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(loc -> {
                            if (!isAdded()) return;
                            if (loc != null) {
                                Float accuracy = loc.hasAccuracy() ? loc.getAccuracy() : null;
                                writeEntrantToFirestore(currentUser,
                                        loc.getLatitude(), loc.getLongitude(), accuracy);
                            } else {
                                onNoLocation(required, currentUser);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (!isAdded()) return;
                            onNoLocation(required, currentUser);
                        });
            } catch (SecurityException e) {
                if (!isAdded()) return;
                Log.e(TAG, "location permission missing during last-location fallback", e);
                onNoLocation(required, currentUser);
            }
        }, 10_000L);

        try {
            fusedLocationClient.requestLocationUpdates(
                    freshRequest, callbackHolder[0], Looper.getMainLooper())
                    .addOnFailureListener(e -> {
                        timeoutHandler.removeCallbacksAndMessages(null);
                        if (!isAdded()) return;
                        Log.w(TAG, "Failed to request fused location updates", e);
                        onNoLocation(required, currentUser);
                    });
        } catch (SecurityException e) {
            timeoutHandler.removeCallbacksAndMessages(null);
            Log.e(TAG, "location permission missing", e);
            onNoLocation(required, currentUser);
        }
    }

    private void onNoLocation(boolean required, @NonNull Users currentUser) {
        if (required) {
            btnWaitlist.setEnabled(true);
            Toast.makeText(requireContext(),
                    "Could not read your location. Check Google Play services or try again.",
                    Toast.LENGTH_LONG).show();
        } else {
            writeEntrantToFirestore(currentUser, 0.0, 0.0, null);
        }
    }

    private boolean isGooglePlayServicesLocationAvailable() {
        if (!isAdded()) {
            return false;
        }
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        return status == ConnectionResult.SUCCESS;
    }

    private void writeEntrantToFirestore(@NonNull Users currentUser, double latitude, double longitude,
                                       @Nullable Float accuracyMeters) {
        String docId = currentUser.getId();
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("id", docId);
        entrantData.put("eventId", eventId);
        entrantData.put("name", currentUser.getName());
        entrantData.put("email", currentUser.getEmail());
        entrantData.put("status", "APPLIED");
        entrantData.put("userId", currentUser.getId());
        entrantData.put("statusCode", 0);
        entrantData.put("latitude", latitude);
        entrantData.put("longitude", longitude);
        entrantData.put("locationUpdatedAt", FieldValue.serverTimestamp());
        if (accuracyMeters != null && accuracyMeters > 0f && !(latitude == 0.0 && longitude == 0.0)) {
            entrantData.put("locationAccuracy", accuracyMeters);
        }

        db.collection("events").document(eventId)
                .collection("entrants").document(docId)
                .set(entrantData)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    isOnWaitlist = true;
                    currentEntrantDocId = docId;
                    notificationHelper.sendWaitlistedNotification(currentUser.getId(), eventId);
                    waitlistCount++;
                    refreshWaitlistButtonState();
                    updateWaitlistCounter();
                    btnWaitlist.setEnabled(true);
                    Toast.makeText(requireContext(), "Joined the waitlist!", Toast.LENGTH_SHORT).show();
                    writeEventHistoryForCurrentUser(currentUser.getId());
                    startWaitlistLocationSharingIfNeeded();
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
        stopWaitlistLocationSharing();

        db.collection("events").document(eventId)
                .collection("entrants").document(currentEntrantDocId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    isOnWaitlist = false;
                    currentEntrantDocId = null;
                    waitlistCount = Math.max(0, waitlistCount - 1);
                    refreshWaitlistButtonState();
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
     * Fetches comments for the current event from Firestore.
     */
    private void loadComments() {
        db.collection("events").document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        Log.e(TAG, "Error loading comments", error);
                        return;
                    }

                    commentList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.setId(doc.getId());
                                commentList.add(comment);
                            }
                        }
                    }
                    commentAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Posts a new comment for the current event to Firestore.
     */
    private void postComment() {
        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) return;

        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to post a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment comment = new Comment();
        comment.setUserId(currentUser.getId());
        comment.setUserName(currentUser.getName());
        comment.setText(content);
        comment.setTimestamp(new Date());

        db.collection("events").document(eventId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    etComment.setText("");
                    Toast.makeText(getContext(), "Comment posted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error posting comment", e));
    }

    /**
     * Deletes a comment from Firestore. Only allowed for the event host.
     * @param comment The comment to delete.
     */
    private void deleteComment(Comment comment) {
        if (comment.getId() == null) return;

        db.collection("events").document(eventId)
                .collection("comments").document(comment.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Comment deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error deleting comment", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting comment", e);
                });
    }

    /**
     * Updates the text and color of the waitlist button based on whether the user is on the waitlist.
     */
    private void refreshWaitlistButtonState() {
        if (userIsOrganizer) {
            btnWaitlist.setText("Organizer Access");
            btnWaitlist.setEnabled(false);
            btnWaitlist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.dark_grey, null)));
            return;
        }

        if (isPrivateEvent && !isOnWaitlist) {
            btnWaitlist.setText("Private Event By Invitation");
            btnWaitlist.setEnabled(false);
            btnWaitlist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.dark_grey, null)));
            return;
        }

        btnWaitlist.setEnabled(true);
        if (isOnWaitlist) {
            btnWaitlist.setText("Leave Waitlist");
            btnWaitlist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.colorDanger, null)));
        } else if (waitlistCapacity > 0 && waitlistCount >= waitlistCapacity){
            btnWaitlist.setText("Waitlist Full");
            btnWaitlist.setEnabled(false);
            btnWaitlist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.dark_grey, null)));
        } else {
            btnWaitlist.setText("Join Waitlist");
            btnWaitlist.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.colorPrimary, null)));
        }
    }

    /**
     * Updates the waitlist counter text view with current statistics.
     */
    private void updateWaitlistCounter() {
        if (waitlistCapacity > 0) {
            tvWaitlistCounter.setText(waitlistCount + "/" + waitlistCapacity);
        } else {
            tvWaitlistCounter.setText(String.valueOf(waitlistCount));
        }
    }


    /**
     * Fetches the host's display name from Firestore and updates the host section.
     * Format: "FirstName L." with avatar showing the first letter.
     */
    private void resolveHostName(String hostId) {
        if (hostId == null || hostId.isEmpty()) return;

        db.collection("users").document(hostId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null || !doc.exists()) return;

                    String first = doc.getString("firstName");
                    String last = doc.getString("lastName");
                    if (first == null || first.isEmpty()) {
                        String fullName = doc.getString("name");
                        if (fullName != null && !fullName.isEmpty()) {
                            String[] parts = fullName.trim().split("\\s+");
                            first = parts[0];
                            last = parts.length > 1 ? parts[parts.length - 1] : null;
                        }
                    }

                    if (first != null && !first.isEmpty()) {
                        String display = (last != null && !last.isEmpty())
                                ? first + " " + last.charAt(0) + "."
                                : first;
                        tvHostName.setText(display);

                        String picUrl = doc.getString("profilePictureUrl");
                        if (picUrl != null && !picUrl.isEmpty()) {
                            ivHostPicture.setVisibility(View.VISIBLE);
                            tvHostAvatar.setVisibility(View.GONE);
                            Glide.with(this).load(picUrl).circleCrop().into(ivHostPicture);
                        } else {
                            ivHostPicture.setVisibility(View.GONE);
                            tvHostAvatar.setVisibility(View.VISIBLE);
                            tvHostAvatar.setText(String.valueOf(first.charAt(0)).toUpperCase());
                        }
                    }
                });
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

    private boolean isCurrentUserOrganizer(@NonNull DocumentSnapshot eventDoc) {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return false;
        }

        String userId = currentUser.getId();
        String createdBy = eventDoc.getString("createdBy");
        if (userId.equals(createdBy)) {
            return true;
        }

        List<String> coOrganizerIds = FirestoreDataUtils.getStringList(eventDoc, "coOrganizerIds");
        return coOrganizerIds != null && coOrganizerIds.contains(userId);
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
                    Boolean geo = doc.getBoolean("geolocationRequired");
                    eventData.put("geolocationRequired", geo != null && geo);
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
                        tvRegistrationRange.setText(
                                formatDate(doc, "registration_start", "TBD")
                                + " - "
                                + formatDate(doc, "registration_end", "TBD"));

                        Long amountLong = doc.getLong("amount");
                        eventCapacity = (amountLong != null) ? amountLong.intValue() : 0;
                        tvCapacity.setText(String.valueOf(eventCapacity));

                        String posterUrl = doc.getString("posterUrl");
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            Glide.with(this).load(posterUrl).into(ivPoster);
                        } else {
                            ivPoster.setImageResource(android.R.color.darker_gray);
                        }

                        String location = doc.getString("location");
                        if (location != null && !location.isEmpty()) {
                            tvLocation.setText(location);
                        } else {
                            tvLocation.setText("No location set");
                        }

                        String createdBy = doc.getString("createdBy");
                        if (createdBy != null) {
                            resolveHostName(createdBy);
                        }
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
