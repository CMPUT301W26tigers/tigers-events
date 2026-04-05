package com.example.eventsapp;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A fragment that allows users to create a new event.
 * Handles user stories for event creation (US 02.01.01) including:
 * - Setting event description and poster.
 * - Generating a unique promotional QR code for the event.
 * - Specifying event capacity and lottery sample size.
 */
public class CreateEventFragment extends Fragment {

    private static final int QR_SIZE = 400;
    private static final String STATE_EVENT_ID = "state_event_id";
    private static final String STATE_DRAFT_SAVED = "state_draft_saved";

    private TextInputEditText editName;
    private TextInputEditText editDescription;
    private TextInputEditText editCapacity; // This may be obsolete
    private TextInputEditText editEventCapacity;
    private TextInputEditText editSampleSize;
    private TextInputEditText editLocation;
    private ImageView ivPoster;
    private ImageView ivQR;
    private FirebaseFirestore db;
    private TextInputEditText editEventDate;
    private TextInputEditText editRegistrationStart;
    private TextInputEditText editRegistrationEnd;
    private MaterialButton btnTogglePrivateEvent;
    private View shareTitle;
    private View shareQrRow;
    private View shareLinkRow;
    private View shareQrImage;
    private View shareEventLink;

    // Removed from create event page
//    private MaterialButton btnViewWaitlist;
//    private MaterialButton btnManageEnrolledList;
    private MaterialButton btnInviteCoOrganizer;
    private boolean isPrivateEvent;
    private MaterialSwitch switchGeolocationRequired;
    private Uri posterUri;
    private String eventId;
    private boolean draftSaved;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    posterUri = uri;
                    Glide.with(this).load(posterUri).into(ivPoster);
                    View placeholder = getView().findViewById(R.id.tv_poster_placeholder);
                    if (placeholder != null) placeholder.setVisibility(View.GONE);
                }
            }
    );

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
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    /**
     * Restores the stable event UUID from saved instance state (so a screen rotation does not
     * silently generate a second event document), or generates a fresh UUID if this is a first
     * creation.
     *
     * @param savedInstanceState Previously saved state containing the event ID and draft flag,
     *                           or {@code null} on first launch.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            eventId = savedInstanceState.getString(STATE_EVENT_ID);
            draftSaved = savedInstanceState.getBoolean(STATE_DRAFT_SAVED, false);
        }
        if (eventId == null || eventId.trim().isEmpty()) {
            eventId = java.util.UUID.randomUUID().toString();
        }
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
        editCapacity = view.findViewById(R.id.edit_capacity); // This may be obsolete
        editEventCapacity = view.findViewById(R.id.edit_event_capacity);
        editSampleSize = view.findViewById(R.id.edit_sample_size);
        editLocation = view.findViewById(R.id.edit_location);
        ivPoster = view.findViewById(R.id.iv_poster);
        ivQR = view.findViewById(R.id.iv_qr);
        editEventDate = view.findViewById(R.id.edit_event_date);
        editRegistrationStart = view.findViewById(R.id.edit_registration_start);
        editRegistrationEnd = view.findViewById(R.id.edit_registration_end);
        btnTogglePrivateEvent = view.findViewById(R.id.btn_toggle_private_event);
        shareTitle = view.findViewById(R.id.tv_share);
        shareQrRow = ((View) view.findViewById(R.id.btn_share_qr)).getParent() instanceof View
                ? (View) ((View) view.findViewById(R.id.btn_share_qr)).getParent() : null;
        shareLinkRow = ((View) view.findViewById(R.id.btn_share_link)).getParent() instanceof View
                ? (View) ((View) view.findViewById(R.id.btn_share_link)).getParent() : null;
        shareQrImage = view.findViewById(R.id.iv_qr);
        shareEventLink = view.findViewById(R.id.tv_event_link);
//        btnViewWaitlist = view.findViewById(R.id.btn_view_waitlist);
//        btnManageEnrolledList = view.findViewById(R.id.btn_manage_enrolled_list);
        btnInviteCoOrganizer = view.findViewById(R.id.btn_invite_coorganizer);
        switchGeolocationRequired = view.findViewById(R.id.switch_geolocation_required);
        setupDatePickers();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Apply underline to section headers programmatically (paintFlags not available in XML)
        int[] sectionHeaderIds = {
                R.id.tv_section_name, R.id.tv_section_description,
                R.id.tv_section_logistics, R.id.tv_share
        };
        for (int id : sectionHeaderIds) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        // Create new event - generate unique ID and QR
        Event event = new Event(eventId, "", 1, "", "", "", "", "",  0);
        event.setId(eventId);
        updateQRCode(event);

        // Show the real deep link for this event
        android.widget.TextView tvEventLink = view.findViewById(R.id.tv_event_link);
        tvEventLink.setText(event.getEventDeepLink());
        updatePrivateEventUi(event);
        btnTogglePrivateEvent.setOnClickListener(v -> {
            isPrivateEvent = !isPrivateEvent;
            updatePrivateEventUi(event);
        });

        // Poster pencil icon opens image picker
        view.findViewById(R.id.btn_edit_poster).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Pencil icons focus corresponding fields
        view.findViewById(R.id.btn_edit_name).setOnClickListener(v -> {
            editName.requestFocus();
            editName.setSelection(editName.length());
        });
        view.findViewById(R.id.btn_edit_description).setOnClickListener(v -> {
            editDescription.requestFocus();
            editDescription.setSelection(editDescription.length());
        });
        view.findViewById(R.id.btn_edit_logistics).setOnClickListener(v -> {
            editEventDate.performClick();
        });

        // Save event and go to waitlist (chosen entrants)
//        btnViewWaitlist.setOnClickListener(v -> saveAndNavigateToWaitlist(event));
//        if (btnManageEnrolledList != null) {
//            btnManageEnrolledList.setOnClickListener(v -> saveAndNavigateToEnrolled(event));
//        }
        if (btnInviteCoOrganizer != null) {
            btnInviteCoOrganizer.setOnClickListener(v -> ensureDraftSaved(event, () -> {
                if (!isAdded()) {
                    return;
                }
                CoOrganizerInviteHelper.showInviteDialog(this, db, event.getId());
            }));
        }

        // Done: save and go back to Your Events
        view.findViewById(R.id.btn_done).setOnClickListener(v -> saveAndGoBack(event));

        // Share QR — regenerate with the event's real deep link
        view.findViewById(R.id.btn_share_qr).setOnClickListener(v -> {
            String deepLink = event.getEventDeepLink();
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, deepLink);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share QR link"));
        });
    }

    /**
     * Persists the stable event UUID and the {@link #draftSaved} flag so they survive
     * configuration changes without triggering duplicate Firestore writes.
     *
     * @param outState The bundle in which to place saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EVENT_ID, eventId);
        outState.putBoolean(STATE_DRAFT_SAVED, draftSaved);
    }

    /**
     * Attaches DatePicker dialogs to the event date and registration date input fields.
     *
     * When a field is clicked a DatePickerDialog allows the organizer to select a date.
     *
     * Dates are formatted as YYYY-MM-DD and inserted into the corresponding input field.
     */
    private void setupDatePickers() {
        editEventDate.setOnClickListener(v -> showDatePicker(editEventDate));
        editRegistrationStart.setOnClickListener(v -> showDatePicker(editRegistrationStart));
        editRegistrationEnd.setOnClickListener(v -> showDatePicker(editRegistrationEnd));

        editEventDate.setFocusable(false);
        editRegistrationStart.setFocusable(false);
        editRegistrationEnd.setFocusable(false);
    }

    /**
     * Displays a DatePicker dialog for selecting a date.
     *
     * The selected date is formatted YYYY-MM-DD and placed into TextInputEditText.
     *
     * @param target the input field that will receive the selected date
     */
    private void showDatePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    target.setText(date);
                    target.setError(null);
                },
                year, month, day
        );

        picker.show();
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
     * Synchronises the visibility of the sharing section (QR code, link, share buttons) and the
     * label on the private-event toggle button with the current value of {@link #isPrivateEvent}.
     *
     * <p>For private events the QR image is cleared and a placeholder message is shown in place
     * of the deep link.  For public events the QR is regenerated via {@link #updateQRCode(Event)}.
     *
     * @param event The event being created, used to derive the current deep-link URL.
     */
    private void updatePrivateEventUi(Event event) {
        int visibility = isPrivateEvent ? View.GONE : View.VISIBLE;
        shareTitle.setVisibility(visibility);
        if (shareQrRow != null) {
            shareQrRow.setVisibility(visibility);
        }
        if (shareLinkRow != null) {
            shareLinkRow.setVisibility(visibility);
        }
        shareQrImage.setVisibility(visibility);
        shareEventLink.setVisibility(visibility);
//        if (btnViewWaitlist != null) {
//            btnViewWaitlist.setText("Manage Waitlist");
//        }
//        if (btnManageEnrolledList != null) {
//            btnManageEnrolledList.setText("Manage Enrolled");
//        }
        if (btnTogglePrivateEvent != null) {
            btnTogglePrivateEvent.setText(isPrivateEvent ? "Set Event Public" : "Set Event Private");
        }

        TextView tvEventLink = requireView().findViewById(R.id.tv_event_link);
        if (isPrivateEvent) {
            ivQR.setImageDrawable(null);
            tvEventLink.setText("Private events cannot be shared publicly.");
        } else {
            updateQRCode(event);
            tvEventLink.setText(event.getEventDeepLink());
        }
    }


//    private void saveAndNavigateToWaitlist(Event event) {
//        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
//        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
//        // String capacityStr = editCapacity.getText() != null ? editCapacity.getText().toString().trim() : "1"; // May be obsolete
//        String eventCapacityStr = editEventCapacity != null && editEventCapacity.getText() != null ? editEventCapacity.getText().toString().trim() : "";
//        String waitlistCapacityStr = editCapacity.getText() != null ? editCapacity.getText().toString().trim() : "";
//        String sampleStr = editSampleSize != null && editSampleSize.getText() != null
//                ? editSampleSize.getText().toString().trim() : "0";
//
//        String eventDate = getText(editEventDate);
//        String registrationStart = getText(editRegistrationStart);
//        String registrationEnd = getText(editRegistrationEnd);
//
//
//        // may be obsolete
//        // if (capacityStr.isEmpty()) {
//        //    Toast.makeText(requireContext(), "Capacity required", Toast.LENGTH_SHORT).show();
//        //    return;
//        //}
//        if (eventCapacityStr.isEmpty()) {
//            TigerToast.show(requireContext(), "Event Capacity required", Toast.LENGTH_SHORT);
//            return;
//        }
//
//        if (eventDate.isEmpty()) {
//            editEventDate.setError("Event date required");
//            return;
//        }
//
//        if (registrationStart.isEmpty()) {
//            editRegistrationStart.setError("Registration start required");
//            return;
//        }
//
//        if (registrationEnd.isEmpty()) {
//            editRegistrationEnd.setError("Registration end required");
//            return;
//        }
//
////        int capacity; may be obsolete
//        int eventCapacityVal;
//        int waitlistCapacityVal = 0;
//        int sampleSize;
//        try {
//            // May be obsolete
//            // capacity = Integer.parseInt(capacityStr);
//            // if (capacity <= 0) {
//            //    Toast.makeText(requireContext(), "Capacity must be positive", Toast.LENGTH_SHORT).show();
//            //    return;
//            // }
//            eventCapacityVal = Integer.parseInt(eventCapacityStr);
//                if (eventCapacityVal <= 0) {
//                    TigerToast.show(requireContext(), "Event Capacity must be positive", Toast.LENGTH_SHORT);
//                return;
//            }
//            // sampleSize = Integer.parseInt(sampleStr);
//            // if (sampleSize < 0) sampleSize = 0;
//        } catch (NumberFormatException e) {
//            TigerToast.show(requireContext(), "Invalid Event Capacity", Toast.LENGTH_SHORT);
//            return;
//        }
//        if (!waitlistCapacityStr.isEmpty()) {
//            try {
//                waitlistCapacityVal = Integer.parseInt(waitlistCapacityStr);
//                if (waitlistCapacityVal < 0) waitlistCapacityVal = 0;
//            } catch (NumberFormatException e) {
//                TigerToast.show(requireContext(), "Invalid Waitlist Capacity", Toast.LENGTH_SHORT);
//                return;
//            }
//        }
//        try {
//            sampleSize = Integer.parseInt(sampleStr);
//            if (sampleSize < 0) sampleSize = 0;
//        } catch (NumberFormatException e) {
//            sampleSize = 0;
//        }
//
//        if (!isValidRegistrationPeriod(eventDate, registrationStart, registrationEnd)) {
//            return;
//        }
//
//        if (name.isEmpty()) {
//            TigerToast.show(requireContext(), "Event name required", Toast.LENGTH_SHORT);
//            return;
//        }
//
//        event.setName(name);
//        event.setDescription(description);
////        event.setAmount(capacity); may be obsolete
//        event.setAmount(eventCapacityVal);
//        event.setWaitlistCapacity(waitlistCapacityVal);
//        event.setSampleSize(sampleSize);
//        event.setEvent_date(eventDate);
//        event.setRegistration_start(registrationStart);
//        event.setRegistration_end(registrationEnd);
//        if (!isPrivateEvent) {
//            updateQRCode(event);
//        }
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("id", event.getId());
//        data.put("name", event.getName());
//        data.put("amount", event.getAmount());
//        data.put("waitlistCapacity", event.getWaitlistCapacity());
//        data.put("description", event.getDescription());
//        data.put("posterUrl", event.getPosterUrl());
//        data.put("sampleSize", event.getSampleSize());
//        data.put("event_date", event.getEvent_date());
//        data.put("registration_start", event.getRegistration_start());
//        data.put("registration_end", event.getRegistration_end());
//        data.put("isPrivate", isPrivateEvent);
//        data.put("coOrganizerIds", new ArrayList<String>());
//        data.put("pendingCoOrganizerIds", new ArrayList<String>());
//        data.put("geolocationRequired", switchGeolocationRequired != null && switchGeolocationRequired.isChecked());
//
//        // Store who created this event for filtering on the Events page
//        Users currentUser = UserManager.getInstance().getCurrentUser();
//        if (currentUser != null && currentUser.getId() != null) {
//            data.put("createdBy", currentUser.getId());
//        }
//        ensureDraftSaved(event, () -> {
//            if (!isAdded()) {
//                return;
//            }
//            Bundle args = new Bundle();
//            args.putString("eventId", event.getId());
//            args.putString("eventName", event.getName());
//            Navigation.findNavController(requireView())
//                    .navigate(R.id.viewEntrantsFragment, args);
//        });
//    }

//    private void saveAndNavigateToEnrolled(Event event) {
//        ensureDraftSaved(event, () -> {
//            if (!isAdded()) {
//                return;
//            }
//            Bundle args = new Bundle();
//            args.putString("eventId", event.getId());
//            Navigation.findNavController(requireView())
//                    .navigate(R.id.enrolledFragment, args);
//        });
//    }

    /**
     * Validates inputs, saves the event to Firestore, then returns to the previous screen.
     * This is used by the \"Done\" button for a natural completion flow.
     */
    private void saveAndGoBack(Event event) {
        Map<String, Object> data = buildEventData(event);
        if (data == null) {
            return;
        }
        Users currentUser = UserManager.getInstance().getCurrentUser();
        saveEventToFirestore(event, data, currentUser, () -> {
            if (!isAdded()) {
                return;
            }
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    /**
     * Guarantees the event has been persisted to Firestore at least once before running
     * {@code onSuccess}.
     *
     * <p>If {@link #draftSaved} is already {@code true} the callback is invoked immediately
     * without a Firestore round-trip.  Otherwise {@link #buildEventData(Event)} is called to
     * validate and build the payload, then {@link #saveEventToFirestore} is called.
     *
     * @param event     The event to persist if not yet saved.
     * @param onSuccess Callback to invoke after a successful save (or immediately if already
     *                  saved); never called if validation or saving fails.
     */
    private void ensureDraftSaved(Event event, @NonNull Runnable onSuccess) {
        if (draftSaved) {
            onSuccess.run();
            return;
        }

        Map<String, Object> data = buildEventData(event);
        if (data == null) {
            return;
        }

        Users currentUser = UserManager.getInstance().getCurrentUser();
        saveEventToFirestore(event, data, currentUser, onSuccess);
    }

    /**
     * Validates all input fields and, on success, mutates the {@code event} object and builds a
     * Firestore-ready field map.
     *
     * <p>Returns {@code null} (after showing an appropriate error message) if any required field
     * is empty, any numeric value is unparseable or out of range, or the registration period is
     * invalid.
     *
     * @param event The event object to mutate with the validated field values.
     * @return A {@link Map} ready to pass to {@code DocumentReference.set()}, or {@code null} if
     *         validation failed.
     */
    @Nullable
    private Map<String, Object> buildEventData(Event event) {
        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
        // String capacityStr = editCapacity.getText() != null ? editCapacity.getText().toString().trim() : "1"; may be obsolete
        String eventCapacityStr = editEventCapacity != null && editEventCapacity.getText() != null ? editEventCapacity.getText().toString().trim() : "";
        String waitlistCapacityStr = editCapacity.getText() != null ? editCapacity.getText().toString().trim() : "";
        String sampleStr = editSampleSize != null && editSampleSize.getText() != null
                ? editSampleSize.getText().toString().trim() : "";

        String eventDate = getText(editEventDate);
        String registrationStart = getText(editRegistrationStart);
        String registrationEnd = getText(editRegistrationEnd);

        if (name.isEmpty()) {
            TigerToast.show(requireContext(), "Event name required", Toast.LENGTH_SHORT);
            return null;
        }
        // May be obsolete
//        if (capacityStr.isEmpty()) {
//            Toast.makeText(requireContext(), "Capacity required", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if (eventCapacityStr.isEmpty()) {
            TigerToast.show(requireContext(), "Event Capacity required", Toast.LENGTH_SHORT);
            return null;
        }

        if (eventDate.isEmpty()) {
            editEventDate.setError("Event date required");
            return null;
        }
        if (registrationStart.isEmpty()) {
            editRegistrationStart.setError("Registration start required");
            return null;
        }
        if (registrationEnd.isEmpty()) {
            editRegistrationEnd.setError("Registration end required");
            return null;
        }

//        int capacity;
        int eventCapacityVal;
        int waitlistCapacityVal = 0;
        int sampleSize;
        try {
            // may be obsolete
//            capacity = Integer.parseInt(capacityStr);
//            if (capacity <= 0) {
//                Toast.makeText(requireContext(), "Capacity must be positive", Toast.LENGTH_SHORT).show();
//                return;
//            }
            eventCapacityVal = Integer.parseInt(eventCapacityStr);
            if (eventCapacityVal <= 0) {
                TigerToast.show(requireContext(), "Event Capacity must be positive", Toast.LENGTH_SHORT);
                return null;
            }
//            sampleSize = Integer.parseInt(sampleStr);
//            if (sampleSize < 0) sampleSize = 0;
        } catch (NumberFormatException e) {
            TigerToast.show(requireContext(), "Invalid Event Capacity", Toast.LENGTH_SHORT);
            return null;
        }

        if (!waitlistCapacityStr.isEmpty()) {
            try {
                waitlistCapacityVal = Integer.parseInt(waitlistCapacityStr);
                if (waitlistCapacityVal < 0) waitlistCapacityVal = 0;
            } catch (NumberFormatException e) {
                TigerToast.show(requireContext(), "Invalid Waitlist Capacity", Toast.LENGTH_SHORT);
                return null;
            }
        }
        if (sampleStr.isEmpty()) {
            sampleSize = eventCapacityVal;
        } else {
            try {
                sampleSize = Integer.parseInt(sampleStr);
                // If they enter 0 or a negative number, safely fall back to event capacity
                if (sampleSize <= 0) sampleSize = eventCapacityVal;
            } catch (NumberFormatException e) {
                sampleSize = eventCapacityVal;
            }
        }

        if (!isValidRegistrationPeriod(eventDate, registrationStart, registrationEnd)) {
            return null;
        }

        event.setId(eventId);
        event.setName(name);
        event.setDescription(description);
//        event.setAmount(capacity); may be obsolete
        event.setAmount(eventCapacityVal);
        event.setWaitlistCapacity(waitlistCapacityVal);
        event.setSampleSize(sampleSize);
        event.setEvent_date(eventDate);
        event.setRegistration_start(registrationStart);
        event.setRegistration_end(registrationEnd);
        if (!isPrivateEvent) {
            updateQRCode(event);
        }

        String location = editLocation != null && editLocation.getText() != null
                ? editLocation.getText().toString().trim() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("id", event.getId());
        data.put("name", event.getName());
        data.put("amount", event.getAmount());
        data.put("waitlistCapacity", event.getWaitlistCapacity());
        data.put("description", event.getDescription());
        data.put("posterUrl", event.getPosterUrl());
        data.put("sampleSize", event.getSampleSize());
        data.put("event_date", event.getEvent_date());
        data.put("registration_start", event.getRegistration_start());
        data.put("registration_end", event.getRegistration_end());
        data.put("location", location);
        data.put("isPrivate", isPrivateEvent);
        if (!draftSaved) {
            data.put("coOrganizerIds", new ArrayList<String>());
            data.put("pendingCoOrganizerIds", new ArrayList<String>());
        }
        data.put("geolocationRequired", switchGeolocationRequired != null && switchGeolocationRequired.isChecked());

        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            data.put("createdBy", currentUser.getId());
        }

        return data;
    }

    /**
     * Uploads the poster image (if one was selected) before writing the event document to
     * Firestore.  If the upload fails the event is still saved without a poster URL so the
     * organizer is not blocked.
     *
     * @param event       The event being saved (used to derive the Storage path).
     * @param data        The Firestore field map; {@code posterUrl} may be mutated with the
     *                    Storage download URL on a successful upload.
     * @param currentUser The signed-in user; may be {@code null} if called before sign-in
     *                    completes (unlikely in normal flow).
     * @param onSuccess   Callback invoked after the event document is successfully written.
     */
    private void saveEventToFirestore(Event event, Map<String, Object> data, @Nullable Users currentUser,
                                      @NonNull Runnable onSuccess) {
        if (posterUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("posters/" + event.getId() + ".jpg");
            storageRef.putFile(posterUri)
                    .addOnSuccessListener(taskSnapshot ->
                            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                data.put("posterUrl", downloadUri.toString());
                                writeEventToFirestore(event, data, currentUser, onSuccess);
                            }).addOnFailureListener(e -> {
                                Log.e("CreateEvent", "Failed to get download URL", e);
                                writeEventToFirestore(event, data, currentUser, onSuccess);
                            })
                    )
                    .addOnFailureListener(e -> {
                        Log.e("CreateEvent", "Failed to upload poster", e);
                        if (isAdded()) {
                            TigerToast.show(requireContext(), "Failed to upload poster", Toast.LENGTH_SHORT);
                        }
                        writeEventToFirestore(event, data, currentUser, onSuccess);
                    });
        } else {
            writeEventToFirestore(event, data, currentUser, onSuccess);
        }
    }

    /**
     * Writes (or overwrites) the event document in Firestore and, on success, marks
     * {@link #draftSaved} as {@code true}, writes an organizer history record for the current
     * user, and invokes {@code onSuccess}.
     *
     * @param event       The event whose ID is used as the Firestore document ID.
     * @param data        The full field map to persist.
     * @param currentUser The signed-in user for whom to write an organizer history record;
     *                    history is skipped if {@code null}.
     * @param onSuccess   Callback invoked after a successful Firestore write.
     */
    private void writeEventToFirestore(Event event, Map<String, Object> data, @Nullable Users currentUser,
                                       @NonNull Runnable onSuccess) {
        DocumentReference docRef = db.collection("events").document(event.getId());
        docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) {
                        return;
                    }
                    Log.d("CreateEvent", "Event saved");
                    draftSaved = true;
                    TigerToast.show(requireContext(), "Event saved", Toast.LENGTH_SHORT);
                    if (currentUser != null && currentUser.getId() != null) {
                        EventCleanupHelper.writeHistoryRecord(currentUser.getId(), event.getId(), data, "ORGANIZED");
                    }
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    Log.e("CreateEvent", "Failed to save", e);
                    TigerToast.show(requireContext(), "Failed to save event", Toast.LENGTH_SHORT);
                });
    }

    /**
     * Validates the event registration period.
     *
     * Ensures that:
     * - registration start occurs before registration end
     * - registration end occurs before the event date
     *
     * Dates must be in YYYY-MM-DD format.
     *
     * @param eventDateStr event date string
     * @param registrationStartStr registration start date
     * @param registrationEndStr registration end date
     *
     * @return true if the registration period is valid, false otherwise
     */
    private boolean isValidRegistrationPeriod(String eventDateStr,
                                              String registrationStartStr,
                                              String registrationEndStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        sdf.setLenient(false);

        try {
            Date eventDate = sdf.parse(eventDateStr);
            Date registrationStart = sdf.parse(registrationStartStr);
            Date registrationEnd = sdf.parse(registrationEndStr);

            if (eventDate == null || registrationStart == null || registrationEnd == null) {
                TigerToast.show(requireContext(), "Invalid date entered", Toast.LENGTH_SHORT);
                return false;
            }

            if (registrationStart.after(registrationEnd)) {
                editRegistrationStart.setError("Start must be before end");
                editRegistrationEnd.setError("End must be after start");
                return false;
            }

            if (registrationEnd.after(eventDate)) {
                editRegistrationEnd.setError("Registration must end before event date");
                editEventDate.setError("Event date must be after registration ends");
                return false;
            }

        } catch (ParseException e) {
            TigerToast.show(requireContext(), "Date format must be YYYY-MM-DD", Toast.LENGTH_SHORT);
            return false;
        }

        return true;
    }

    /**
     * Safely extracts the trimmed text from a {@link TextInputEditText}.
     *
     * @param editText The input field to read; may be {@code null}.
     * @return The trimmed string, or an empty string if {@code editText} or its content is
     *         {@code null}.
     */
    private String getText(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }
}
