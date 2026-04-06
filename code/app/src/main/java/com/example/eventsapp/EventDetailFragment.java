package com.example.eventsapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private MaterialButton btnWaitlist, btnWaitlist2;

    // Comment views
    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;
    private TextInputEditText etComment;
    private MaterialButton btnPostComment;

    private String preloadPosterUrl = null;
    private String preloadHostPicUrl = null;
    private boolean isOnWaitlist = false;
    private boolean isPrivateEvent = false;
    private boolean userIsOrganizer = false;
    private String currentEntrantDocId = null;
    private String userStatus = null;
    private boolean isRegistrationOpen = true;
    private int waitlistCount = 0;
    private int waitlistCapacity = 0;
    private int eventCapacity = 0;
    private boolean hasPendingGroup = false;
    private String eventCreatorId = null;
    private String eventCreatorEmail = null;
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
                        setWaitlistButtonsEnabled(true);
                        return;
                    }
                    // This callback is only launched when geolocationRequired=true.
                    if (granted) {
                        fetchLocationAndCompleteJoin(true);
                    } else {
                        setWaitlistButtonsEnabled(true);
                        TigerToast.show(requireContext(), "Location permission is required to join this event", Toast.LENGTH_LONG);
                    }
                });
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
            fromHistory = getArguments().getBoolean("fromHistory", false);
            preloadPosterUrl = getArguments().getString("posterUrl");
            preloadHostPicUrl = getArguments().getString("hostProfilePictureUrl");
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
        btnWaitlist2 = view.findViewById(R.id.btnWaitlist2);

        if (preloadPosterUrl != null) {
            Glide.with(this).load(preloadPosterUrl).into(ivPoster);
        }
        if (preloadHostPicUrl != null) {
            ivHostPicture.setVisibility(View.VISIBLE);
            Glide.with(this).load(preloadHostPicUrl).circleCrop().into(ivHostPicture);
        }

        view.findViewById(R.id.btnInfo).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("How joining events work:")
                    .setMessage("Invites are sent randomly to entrants in the waitlist. If you were not chosen, you may are eligible for a re-draw if someone declines their invitation./n For Group joins: The group is treated as one person and the odds are the same.")
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
            setWaitlistButtonsVisibility(View.GONE);
            return;
        }

        if (fromHistory) {
            // Load from user's personal history instead of global events
            setWaitlistButtonsVisibility(View.GONE);
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

            btnWaitlist2.setOnClickListener(v -> {
                if (isOnWaitlist) {
                    leaveWaitlist();
                } else {
                    joinWaitlistWithGroup();
                }
            });
        }

        loadComments();
        btnPostComment.setOnClickListener(v -> postComment());
    }

    private void setWaitlistButtonsEnabled(boolean enabled) {
        if (btnWaitlist != null) btnWaitlist.setEnabled(enabled);
        if (btnWaitlist2 != null) btnWaitlist2.setEnabled(enabled);
    }

    private void setWaitlistButtonsVisibility(int visibility) {
        if (btnWaitlist != null) btnWaitlist.setVisibility(visibility);
        if (btnWaitlist2 != null) btnWaitlist2.setVisibility(visibility);
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
                        // Check if registration is open
                        String regStart = doc.getString("registration_start");
                        String regEnd = doc.getString("registration_end");
                        checkRegistrationPeriod(regStart, regEnd);

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
                        setWaitlistButtonsVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvName.setText("Failed to load event");
                    setWaitlistButtonsVisibility(View.GONE);
                });
    }

    /**
     * Checks if the current date is within the registration start and end range.
     */
    private void checkRegistrationPeriod(String startStr, String endStr) {
        if (startStr == null || endStr == null || startStr.isEmpty() || endStr.isEmpty()) {
            isRegistrationOpen = true;
            return;
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CANADA);
            sdf.setLenient(false);
            Date startDate = sdf.parse(startStr);
            Date endDate = sdf.parse(endStr);

            // Adjust end date to the very end of the day (23:59:59)
            if (endDate != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(endDate);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                cal.set(java.util.Calendar.MINUTE, 59);
                cal.set(java.util.Calendar.SECOND, 59);
                endDate = cal.getTime();
            }

            Date now = new Date();
            isRegistrationOpen = now.compareTo(startDate) >= 0 && now.compareTo(endDate) <= 0;
        } catch (java.text.ParseException e) {
            isRegistrationOpen = true; // Fallback to open if parsing fails
        }
    }
    /**
     * Fetches the current waitlist status for the event and the current user.
     */
    private void loadWaitlistStatus() {
        // Load total waitlist count
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "APPLIED")
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (value != null) {
                        waitlistCount = value.size();
                        updateWaitlistCounter();
                    }
                });

        // Check if current user is already on the waitlist
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            db.collection("events").document(eventId)
                    .collection("entrants")
                    .whereEqualTo("userId", currentUser.getId())
                    .addSnapshotListener((value, error) -> {
                        if (!isAdded()) return;
                        if (value != null && !value.isEmpty()) {
//                            isOnWaitlist = true;
//                            currentEntrantDocId = value.getDocuments().get(0).getId();
//                            refreshWaitlistButtonState();
//                            startWaitlistLocationSharingIfNeeded();
                            DocumentSnapshot doc = value.getDocuments().get(0);
                            userStatus = doc.getString("status");
                            currentEntrantDocId = doc.getId();
                            if ("APPLIED".equals(userStatus)) {
                                isOnWaitlist = true;
                                startWaitlistLocationSharingIfNeeded();
                            } else {
                                isOnWaitlist = false;
                                stopWaitlistLocationSharing();
                            }
                            refreshWaitlistButtonState();
                        } else {
                            isOnWaitlist = false;
                            userStatus = null;
                            currentEntrantDocId = null;
                            refreshWaitlistButtonState();
                            stopWaitlistLocationSharing();
                        }
                    });

            // Check if current user is part of a pending waitlist group
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                db.collection("events").document(eventId)
                        .collection("groups")
                        .whereEqualTo("status", "PENDING")
                        .whereArrayContains("acceptedEmails", currentUser.getEmail())
                        .addSnapshotListener((value, error) -> {
                            if (!isAdded()) return;
                            if (error != null) {
                                Log.e(TAG, "Error loading group status", error);
                                return;
                            }
                            if (value != null && !value.isEmpty()) {
                                hasPendingGroup = true;
                            } else {
                                hasPendingGroup = false;
                            }
                            refreshWaitlistButtonState();
                        });
            }
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
            TigerToast.show(requireContext(), "Please sign in to join the waitlist", Toast.LENGTH_SHORT);
            return;
        }
        if (currentUser.getId() == null || currentUser.getId().isEmpty()) {
            TigerToast.show(requireContext(), "Please sign in to join the waitlist", Toast.LENGTH_SHORT);
            return;
        }

        if (waitlistCapacity > 0 && waitlistCount >= waitlistCapacity && !isOnWaitlist) {
            TigerToast.show(requireContext(), "The waitlist for this event is full", Toast.LENGTH_SHORT);
            return;
        }
        setWaitlistButtonsEnabled(false);

        if (!eventDetailsLoaded) {
            fetchEventSettingsThenContinueJoin(currentUser);
            return;
        }
        continueJoinWithResolvedGeoSetting(currentUser);
    }

    private void joinWaitlistWithGroup() {
        if (userIsOrganizer || isPrivateEvent) {
            return;
        }

        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null || currentUser.getId().isEmpty()) {
            TigerToast.show(requireContext(), "Please sign in to join the waitlist", Toast.LENGTH_SHORT);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Join Waitlist with Group");
        builder.setMessage("Enter email addresses of friends to join with (comma-separated):");

        final EditText input = new EditText(requireContext());
        input.setHint("email1@example.com, email2@example.com");
        builder.setView(input);

        builder.setPositiveButton("Invite", (dialog, which) -> {
            String emailsStr = input.getText().toString().trim();
            if (emailsStr.isEmpty()) {
                TigerToast.show(requireContext(), "Please enter at least one email", Toast.LENGTH_SHORT);
                return;
            }

            List<String> emails = new ArrayList<>();
            for (String email : emailsStr.split(",")) {
                String e = email.trim();
                if (!e.isEmpty()) {
                    if (eventCreatorEmail != null && e.equalsIgnoreCase(eventCreatorEmail)) {
                        TigerToast.show(requireContext(), "You cannot invite the event owner", Toast.LENGTH_SHORT);
                        return;
                    }
                    emails.add(e);
                }
            }

            if (emails.isEmpty()) {
                TigerToast.show(requireContext(), "Please enter valid emails", Toast.LENGTH_SHORT);
                return;
            }

            createWaitlistGroup(currentUser, emails);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createWaitlistGroup(Users currentUser, List<String> emails) {
        String groupId = UUID.randomUUID().toString();
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupId", groupId);
        groupData.put("eventId", eventId);
        groupData.put("creatorId", currentUser.getId());
        groupData.put("creatorEmail", currentUser.getEmail());
        groupData.put("memberEmails", emails);
        
        List<String> acceptedEmails = new ArrayList<>();
        acceptedEmails.add(currentUser.getEmail());
        groupData.put("acceptedEmails", acceptedEmails);
        groupData.put("status", "PENDING");
        groupData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("events").document(eventId)
                .collection("groups").document(groupId)
                .set(groupData)
                .addOnSuccessListener(unused -> {
                    TigerToast.show(requireContext(), "Group invitations sent!", Toast.LENGTH_SHORT);
                    // Send notifications to all invited emails
                    for (String email : emails) {
                        notificationHelper.sendGroupWaitlistInvitationNotification(email, eventId, groupId, currentUser.getName());
                    }
                })
                .addOnFailureListener(e -> {
                    TigerToast.show(requireContext(), "Failed to create group", Toast.LENGTH_SHORT);
                    Log.e(TAG, "Error creating group", e);
                });
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
                        setWaitlistButtonsEnabled(true);
                        TigerToast.show(requireContext(), "Could not load event", Toast.LENGTH_SHORT);
                        return;
                    }
                    applyGeolocationRequiredFromEvent(doc);
                    eventDetailsLoaded = true;
                    continueJoinWithResolvedGeoSetting(currentUser);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setWaitlistButtonsEnabled(true);
                    TigerToast.show(requireContext(), "Could not load event", Toast.LENGTH_SHORT);
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
            // Organizer requires location: ask for permission, then capture GPS.
            if (!hasLocationPermission()) {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
            fetchLocationAndCompleteJoin(true);
        } else {
            // Location is NOT required: join immediately — no permission dialog, no GPS capture.
            writeEntrantToFirestore(currentUser, 0.0, 0.0, null);
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
            setWaitlistButtonsEnabled(true);
            return;
        }

        if (!isAdded()) return;
        if (fusedLocationClient == null || !isGooglePlayServicesLocationAvailable()) {
            Log.w(TAG, "Fused location unavailable; continuing without Play services location");
            onNoLocation(required, currentUser);
            return;
        }
        TigerToast.show(requireContext(), "Getting your location…", Toast.LENGTH_SHORT);

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
            setWaitlistButtonsEnabled(true);
            TigerToast.show(requireContext(), "Could not read your location. Check Google Play services or try again.", Toast.LENGTH_LONG);
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
                    setWaitlistButtonsEnabled(true);
                    TigerToast.show(requireContext(), "Joined the waitlist!", Toast.LENGTH_SHORT);
                    writeEventHistoryForCurrentUser(currentUser.getId());
                    startWaitlistLocationSharingIfNeeded();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setWaitlistButtonsEnabled(true);
                    TigerToast.show(requireContext(), "Failed to join waitlist", Toast.LENGTH_SHORT);
                    Log.e(TAG, "Error joining waitlist", e);
                });
    }

    /**
     * Removes the current user from the event's waitlist in Firestore.
     */
    private void leaveWaitlist() {
        if (currentEntrantDocId == null) return;

        setWaitlistButtonsEnabled(false);
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
                    setWaitlistButtonsEnabled(true);
                    TigerToast.show(requireContext(), "Removed from waitlist", Toast.LENGTH_SHORT);
                    // Delete history record since user voluntarily left
                    Users user = UserManager.getInstance().getCurrentUser();
                    if (user != null && user.getId() != null) {
                        EventCleanupHelper.deleteHistoryRecord(user.getId(), eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    setWaitlistButtonsEnabled(true);
                    TigerToast.show(requireContext(), "Failed to leave waitlist", Toast.LENGTH_SHORT);
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
            TigerToast.show(requireContext(), "Please sign in to post a comment", Toast.LENGTH_SHORT);
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
                    TigerToast.show(getContext(), "Comment posted", Toast.LENGTH_SHORT);
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
                    TigerToast.show(getContext(), "Comment deleted", Toast.LENGTH_SHORT);
                })
                .addOnFailureListener(e -> {
                    TigerToast.show(getContext(), "Error deleting comment", Toast.LENGTH_SHORT);
                    Log.e(TAG, "Error deleting comment", e);
                });
    }

    /**
     * Updates the text and color of the waitlist button based on whether the user is on the waitlist.
     */
    private void refreshWaitlistButtonState() {
        String text;
        String text2 = null;
        int colorRes;
        boolean enabled = true;

        if (userIsOrganizer) {
            text = "Organizer Access";
            enabled = false;
            colorRes = R.color.colorPrimaryDark;

        } else if ("ACCEPTED".equals(userStatus)) {
            text = "Already Accepted";
            text2 = "Already Accepted";
            enabled = false;
            colorRes = R.color.colorPrimaryDark;
        } else if ("INVITED".equals(userStatus)) {
            text = "Already Invited";
            text2 = "Check Inbox";
            enabled = false;
            colorRes = R.color.colorPrimaryDark;

        } else if (isPrivateEvent && !isOnWaitlist) {
            text = "Private Event By Invitation";
            enabled = false;
            colorRes = R.color.colorPrimaryDark;
        } else if (isOnWaitlist) {
            text = getString(R.string.leave_waitlist);
            colorRes = R.color.colorDanger;
            // Hide the second button when on waitlist
            if (btnWaitlist2 != null) btnWaitlist2.setVisibility(View.GONE);
            updateWaitlistButton(btnWaitlist, text, colorRes, enabled);
            return;

        } else if (!isRegistrationOpen) {
            text = "Registration Closed";
            enabled = false;
            colorRes = R.color.colorPrimaryDark;
        } else if (waitlistCapacity > 0 && waitlistCount >= waitlistCapacity){
            text = getString(R.string.waitlist_full);
            enabled = false;
            colorRes = R.color.colorPrimaryDark;
        } else if (hasPendingGroup) {
            text = "Group Pending";
            text2 = "Waiting for Group...";
            enabled = false;
            colorRes = R.color.colorPrimaryDark;
        } else {
            text = getString(R.string.join_waitlist);
            text2 = getString(R.string.join_waitlist_with_group);
            colorRes = R.color.colorPrimary;
        }

        if (text2 == null) text2 = text;
        if (btnWaitlist2 != null) btnWaitlist2.setVisibility(View.VISIBLE);

        updateWaitlistButton(btnWaitlist, text, colorRes, enabled);
        updateWaitlistButton(btnWaitlist2, text2, colorRes, enabled);
    }

    private void updateWaitlistButton(MaterialButton button, String text, int colorRes, boolean enabled) {
        if (button == null) return;
        button.setText(text);
        button.setEnabled(enabled);
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        getResources().getColor(colorRes, null)));
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
                    eventCreatorEmail = doc.getString("email");
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
                            Glide.with(this).load(preloadPosterUrl).into(ivPoster);
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
