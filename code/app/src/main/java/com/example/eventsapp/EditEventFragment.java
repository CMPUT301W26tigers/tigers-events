package com.example.eventsapp;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.Paint;
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
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A fragment that allows an organizer to edit an existing event.
 * Loads the event from Firestore by eventId, pre-fills all fields,
 * and allows saving changes. Also provides access to the waitlist,
 * enrolled entrants, QR code sharing, and event link sharing.
 */
public class EditEventFragment extends Fragment {

    private static final String TAG = "EditEventFragment";
    private static final int QR_SIZE = 400;

    private String eventId;
    private FirebaseFirestore db;

    private TextInputEditText editName;
    private TextInputEditText editDescription;
    private TextInputEditText editCapacity;
    private TextInputEditText editSampleSize;
    private TextInputEditText editEventDate;
    private TextInputEditText editRegistrationStart;
    private TextInputEditText editRegistrationEnd;
    private ImageView ivPoster;
    private ImageView ivQR;
    private TextView tvEventLink;

    /**
     * Called to have the fragment instantiate its user interface view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_event, container, false);
    }

    /**
     * Called immediately after onCreateView has returned. Initializes the UI components,
     * loads the event from Firestore, and sets up listeners.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // Get eventId from navigation arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        // Bind views
        editName = view.findViewById(R.id.edit_name);
        editDescription = view.findViewById(R.id.edit_description);
        editCapacity = view.findViewById(R.id.edit_capacity);
        editSampleSize = view.findViewById(R.id.edit_sample_size);
        editEventDate = view.findViewById(R.id.edit_event_date);
        editRegistrationStart = view.findViewById(R.id.edit_registration_start);
        editRegistrationEnd = view.findViewById(R.id.edit_registration_end);
        ivPoster = view.findViewById(R.id.iv_poster);
        ivQR = view.findViewById(R.id.iv_qr);
        tvEventLink = view.findViewById(R.id.tv_event_link);

        setupDatePickers();

        // Toolbar back navigation
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        // Apply underline to section headers
        int[] sectionHeaderIds = {
                R.id.tv_section_name, R.id.tv_section_description,
                R.id.tv_section_logistics, R.id.tv_share
        };
        for (int id : sectionHeaderIds) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        // Pencil icons focus corresponding fields
        view.findViewById(R.id.btn_edit_name).setOnClickListener(v -> {
            editName.requestFocus();
            editName.setSelection(editName.length());
        });
        view.findViewById(R.id.btn_edit_description).setOnClickListener(v -> {
            editDescription.requestFocus();
            editDescription.setSelection(editDescription.length());
        });
        view.findViewById(R.id.btn_edit_logistics).setOnClickListener(v ->
                editEventDate.performClick());

        // View Waitlist — navigate to ViewEntrantsFragment
        view.findViewById(R.id.btn_view_waitlist).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            args.putString("eventName", getText(editName));
            Navigation.findNavController(requireView())
                    .navigate(R.id.viewEntrantsFragment, args);
        });

        // View Enrolled Entrants — navigate to ViewEntrantsFragment
        view.findViewById(R.id.btn_view_enrolled).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            args.putString("eventName", getText(editName));
            Navigation.findNavController(requireView())
                    .navigate(R.id.viewEntrantsFragment, args);
        });

        // Save button
        view.findViewById(R.id.btn_save).setOnClickListener(v -> saveEvent());

        // Back button
        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        // Share QR
        view.findViewById(R.id.btn_share_qr).setOnClickListener(v -> {
            String deepLink = "tigers-events://event/" + eventId;
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, deepLink);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share QR link"));
        });

        // Share Link
        view.findViewById(R.id.btn_share_link).setOnClickListener(v -> {
            String deepLink = "tigers-events://event/" + eventId;
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, deepLink);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share event link"));
        });

        // Load event data from Firestore
        loadEventFromFirestore();
    }

    /**
     * Loads the event document from Firestore and pre-fills all form fields.
     */
    private void loadEventFromFirestore() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (!doc.exists()) {
                        Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack();
                        return;
                    }

                    // Pre-fill text fields
                    String name = doc.getString("name");
                    String description = doc.getString("description");
                    String eventDate = doc.getString("event_date");
                    String regStart = doc.getString("registration_start");
                    String regEnd = doc.getString("registration_end");
                    Long amountLong = doc.getLong("amount");
                    Long sampleLong = doc.getLong("sampleSize");

                    if (name != null) editName.setText(name);
                    if (description != null) editDescription.setText(description);
                    if (eventDate != null) editEventDate.setText(eventDate);
                    if (regStart != null) editRegistrationStart.setText(regStart);
                    if (regEnd != null) editRegistrationEnd.setText(regEnd);
                    if (amountLong != null) editCapacity.setText(String.valueOf(amountLong.intValue()));
                    if (sampleLong != null) editSampleSize.setText(String.valueOf(sampleLong.intValue()));

                    // Generate QR code from existing event deep link
                    Event tempEvent = new Event(eventId, name != null ? name : "", 1,
                            "", "", "", "", "", 0);
                    tempEvent.setId(eventId);
                    updateQRCode(tempEvent);

                    // Show deep link
                    tvEventLink.setText(tempEvent.getEventDeepLink());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load event", e);
                    Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Attaches DatePicker dialogs to the date input fields.
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
     * Validates all input fields, updates the event in Firestore, and navigates back.
     */
    private void saveEvent() {
        String name = getText(editName);
        String description = getText(editDescription);
        String capacityStr = getText(editCapacity);
        String sampleStr = getText(editSampleSize);
        String eventDate = getText(editEventDate);
        String registrationStart = getText(editRegistrationStart);
        String registrationEnd = getText(editRegistrationEnd);

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Event name required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (capacityStr.isEmpty()) {
            Toast.makeText(requireContext(), "Capacity required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (eventDate.isEmpty()) {
            editEventDate.setError("Event date required");
            return;
        }
        if (registrationStart.isEmpty()) {
            editRegistrationStart.setError("Registration start required");
            return;
        }
        if (registrationEnd.isEmpty()) {
            editRegistrationEnd.setError("Registration end required");
            return;
        }

        int capacity;
        int sampleSize;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                Toast.makeText(requireContext(), "Capacity must be positive", Toast.LENGTH_SHORT).show();
                return;
            }
            sampleSize = sampleStr.isEmpty() ? 0 : Integer.parseInt(sampleStr);
            if (sampleSize < 0) sampleSize = 0;
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid capacity", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidRegistrationPeriod(eventDate, registrationStart, registrationEnd)) {
            return;
        }

        // Build update data map
        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        data.put("name", name);
        data.put("amount", capacity);
        data.put("description", description);
        data.put("sampleSize", sampleSize);
        data.put("event_date", eventDate);
        data.put("registration_start", registrationStart);
        data.put("registration_end", registrationEnd);

        // Preserve createdBy and posterUrl by using update instead of set
        final int finalSampleSize = sampleSize;
        db.collection("events").document(eventId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Event updated", Toast.LENGTH_SHORT).show();

                    // Update organizer history record
                    Users currentUser = UserManager.getInstance().getCurrentUser();
                    if (currentUser != null && currentUser.getId() != null) {
                        // Need posterUrl for history — fetch it from the doc we just updated
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(doc -> {
                                    if (!isAdded()) return;
                                    String posterUrl = doc.getString("posterUrl");
                                    if (posterUrl == null) posterUrl = "";
                                    data.put("posterUrl", posterUrl);
                                    String createdBy = doc.getString("createdBy");
                                    if (createdBy != null) data.put("createdBy", createdBy);
                                    EventCleanupHelper.writeHistoryRecord(
                                            currentUser.getId(), eventId, data, "ORGANIZED");
                                });
                    }

                    Navigation.findNavController(requireView()).popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to update event", e);
                    Toast.makeText(requireContext(), "Failed to update event", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Validates the event registration period.
     *
     * @param eventDateStr event date string
     * @param registrationStartStr registration start date
     * @param registrationEndStr registration end date
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
                Toast.makeText(requireContext(), "Invalid date entered", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "Date format must be YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Helper to safely extract trimmed text from an EditText.
     */
    private String getText(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    /**
     * Determines whether a click from the events list should navigate to
     * the edit page or the detail page.
     *
     * @param isCreatedTab true if the Created tab is selected
     * @param isFromHistory true if the event is from history (expired)
     * @return true if navigation should go to EditEventFragment
     */
    public static boolean shouldNavigateToEdit(boolean isCreatedTab, boolean isFromHistory) {
        return isCreatedTab && !isFromHistory;
    }
}
