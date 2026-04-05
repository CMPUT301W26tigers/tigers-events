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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A fragment that allows users to edit an existing event.
 * Handles user stories for event modification, including:
 * - Updating event details and poster.
 * - Preserving event privacy status while editing other details.
 * - Managing co-organizers.
 */
public class EditEventFragment extends Fragment {

    private static final String TAG = "EditEventFragment";
    private static final int QR_SIZE = 400;

    private String eventId;
    private TextInputEditText editName;
    private TextInputEditText editDescription;
    private TextInputEditText editCapacity;
    private TextInputEditText editSampleSize;
    private TextInputEditText editEventDate;
    private TextInputEditText editRegistrationStart;
    private TextInputEditText editRegistrationEnd;
    private TextInputEditText editLocation;
    private ImageView ivPoster;
    private ImageView ivQR;
    private TextView tvEventLink;
    private MaterialButton btnTogglePrivateEvent;
    private MaterialSwitch switchGeolocationRequired;
    private View shareTitle;
    private MaterialButton btnViewWaitlist;
    private MaterialButton btnManageEnrolledList;
    private MaterialButton btnInviteCoOrganizer;

    private FirebaseFirestore db;
    private Uri posterUri;
    private boolean isPrivateEvent;
    private boolean posterRemoved = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    posterUri = uri;
                    posterRemoved = false;
                    Glide.with(this).load(posterUri).into(ivPoster);
                    View placeholder = getView().findViewById(R.id.tv_poster_placeholder);
                    if (placeholder != null) placeholder.setVisibility(View.GONE);
                }
            }
    );

    /**
     * Helper method to determine if we should navigate to the Edit screen.
     */
    public static boolean shouldNavigateToEdit(boolean isCreatedTab, boolean isFromHistory) {
        return isCreatedTab && !isFromHistory;
    }

    /**
     * Retrieves the {@code eventId} argument passed by the navigation graph so it is available
     * before the view is inflated.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    /**
     * Inflates the edit-event layout.
     *
     * @param inflater           The LayoutInflater to inflate the view hierarchy.
     * @param container          The parent view the fragment UI will attach to, or {@code null}.
     * @param savedInstanceState Previously saved state, or {@code null}.
     * @return The root {@link View} of the fragment's layout.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_event, container, false);
    }

    /**
     * Initialises Firestore, validates that a non-null event ID was supplied, then delegates to
     * {@link #initializeViews(View)}, {@link #loadEventData()}, and {@link #setupToolbar(View)}.
     * Navigates back immediately if the event ID is missing.
     *
     * @param view               The inflated fragment view.
     * @param savedInstanceState Previously saved state, or {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (eventId == null) {
            TigerToast.show(requireContext(), "Error: Event ID missing", Toast.LENGTH_SHORT);
            Navigation.findNavController(view).popBackStack();
            return;
        }

        initializeViews(view);
        loadEventData();
        setupToolbar(view);
    }

    /**
     * Binds all widget references, applies underline styling to section headers, wires click
     * listeners for poster editing/removal, "Done", waitlist management, co-organizer invite,
     * QR sharing, and event deletion.
     *
     * @param view The root view of the fragment's layout.
     */
    private void initializeViews(View view) {
        editName = view.findViewById(R.id.edit_name);
        editDescription = view.findViewById(R.id.edit_description);
        editCapacity = view.findViewById(R.id.edit_capacity);
        editSampleSize = view.findViewById(R.id.edit_sample_size);
        editEventDate = view.findViewById(R.id.edit_event_date);
        editRegistrationStart = view.findViewById(R.id.edit_registration_start);
        editRegistrationEnd = view.findViewById(R.id.edit_registration_end);
        editLocation = view.findViewById(R.id.edit_location);
        ivPoster = view.findViewById(R.id.iv_poster);
        ivQR = view.findViewById(R.id.iv_qr);
        tvEventLink = view.findViewById(R.id.tv_event_link);
        btnTogglePrivateEvent = view.findViewById(R.id.btn_toggle_private_event);
        switchGeolocationRequired = view.findViewById(R.id.switch_geolocation_required);
        shareTitle = view.findViewById(R.id.tv_share);
        btnViewWaitlist = view.findViewById(R.id.btn_view_waitlist);
        btnManageEnrolledList = view.findViewById(R.id.btn_manage_enrolled_list);
        btnInviteCoOrganizer = view.findViewById(R.id.btn_invite_coorganizer);

        setupDatePickers();

        // Underline headers
        int[] headers = {R.id.tv_section_name, R.id.tv_section_description, R.id.tv_section_logistics, R.id.tv_share};
        for (int id : headers) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        view.findViewById(R.id.btn_edit_poster).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        view.findViewById(R.id.btn_remove_poster).setOnClickListener(v -> {
            posterRemoved = true;
            posterUri = null;
            ivPoster.setImageDrawable(null);
            View placeholder = getView().findViewById(R.id.tv_poster_placeholder);
            if (placeholder != null) placeholder.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.btn_done).setOnClickListener(v -> saveEventChanges());

        if (btnTogglePrivateEvent != null) {
            btnTogglePrivateEvent.setVisibility(View.GONE);
        }

        if (btnViewWaitlist != null) {
            btnViewWaitlist.setOnClickListener(v -> openWaitlistManager());
        }
        if (btnManageEnrolledList != null) {
            btnManageEnrolledList.setOnClickListener(v -> openEnrolledManager());
        }
        if (btnInviteCoOrganizer != null) {
            btnInviteCoOrganizer.setOnClickListener(v ->
                    CoOrganizerInviteHelper.showInviteDialog(this, db, eventId));
        }

        view.findViewById(R.id.btn_share_qr).setOnClickListener(v -> shareQR());
        view.findViewById(R.id.btn_share_link).setOnClickListener(v -> shareLink());
        view.findViewById(R.id.btn_delete_event).setOnClickListener(v -> confirmDeleteEvent());
    }

    /**
     * Attaches a navigation-up click listener to the {@link MaterialToolbar} so the back-stack
     * is popped when the user presses the back arrow.
     *
     * @param view The root view used to locate the toolbar.
     */
    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    Navigation.findNavController(view).popBackStack()
            );
        }
    }

    /**
     * Attaches {@link DatePickerDialog} launchers to the event-date, registration-start, and
     * registration-end fields, and makes those fields non-focusable so the software keyboard
     * is never shown for them.
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
     * Shows a {@link DatePickerDialog} pre-initialised to today's date and writes the result into
     * {@code target} in {@code yyyy-MM-dd} format, clearing any existing error on that field.
     *
     * @param target The input field that will receive the selected date string.
     */
    private void showDatePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            String date = String.format(Locale.CANADA, "%04d-%02d-%02d", year, month + 1, day);
            target.setText(date);
            target.setError(null);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Fetches the event document from Firestore and populates all input fields, the poster
     * {@link ImageView}, the private-event toggle, the geolocation switch, and the QR code.
     */
    private void loadEventData() {
        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            if (!doc.exists() || !isAdded()) return;

            editName.setText(doc.getString("name"));
            editDescription.setText(doc.getString("description"));
            editCapacity.setText(String.valueOf(doc.getLong("amount")));
            editSampleSize.setText(String.valueOf(doc.getLong("sampleSize")));
            editEventDate.setText(doc.getString("event_date"));
            editRegistrationStart.setText(doc.getString("registration_start"));
            editRegistrationEnd.setText(doc.getString("registration_end"));
            if (editLocation != null) {
                editLocation.setText(doc.getString("location"));
            }

            isPrivateEvent = Boolean.TRUE.equals(doc.getBoolean("isPrivate"));
            updatePrivateUi();

            if (switchGeolocationRequired != null) {
                switchGeolocationRequired.setChecked(Boolean.TRUE.equals(doc.getBoolean("geolocationRequired")));
            }

            String posterUrl = doc.getString("posterUrl");
            if (posterUrl != null && !posterUrl.isEmpty()) {
                Glide.with(this).load(posterUrl).into(ivPoster);
                View placeholder = getView().findViewById(R.id.tv_poster_placeholder);
                if (placeholder != null) placeholder.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Synchronises the visibility of the QR-code section, event-link text view, and sharing
     * buttons with the current value of {@link #isPrivateEvent}.
     *
     * <p>When the event is public, {@link #updateQRCode()} is also called to regenerate the QR
     * bitmap.  When private, these share elements are hidden.
     */
    private void updatePrivateUi() {
        int visibility = isPrivateEvent ? View.GONE : View.VISIBLE;
        if (shareTitle != null) shareTitle.setVisibility(visibility);
        
        // Hide/show the QR and link rows in the parent layout
        View btnShareQr = getView().findViewById(R.id.btn_share_qr);
        if (btnShareQr != null && btnShareQr.getParent() instanceof View) {
            ((View) btnShareQr.getParent()).setVisibility(visibility);
        }
        
        View btnShareLink = getView().findViewById(R.id.btn_share_link);
        if (btnShareLink != null && btnShareLink.getParent() instanceof View) {
            ((View) btnShareLink.getParent()).setVisibility(visibility);
        }

        if (ivQR != null) ivQR.setVisibility(visibility);
        if (tvEventLink != null) tvEventLink.setVisibility(visibility);

        if (!isPrivateEvent) {
            updateQRCode();
        }
    }

    /**
     * Generates a QR code from the event's deep-link URI ({@code tigers-events://event/<id>})
     * and displays it in {@link #ivQR}.  Also updates the plain-text event-link view.
     */
    private void updateQRCode() {
        String link = "tigers-events://event/" + eventId;
        Bitmap qrBitmap = QRCodeUtil.generateQRCode(link, QR_SIZE, QR_SIZE);
        if (qrBitmap != null && ivQR != null) {
            ivQR.setImageBitmap(qrBitmap);
        }
        if (tvEventLink != null) {
            tvEventLink.setText(link);
        }
    }

    /**
     * Launches the system share sheet with the event's deep-link URI so the organizer can
     * distribute the join link via any installed app.
     */
    private void shareQR() {
        String link = "tigers-events://event/" + eventId;
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, link);
        startActivity(android.content.Intent.createChooser(shareIntent, "Share QR link"));
    }

    /**
     * Shares the event's deep-link URI as plain text.  Currently delegates to {@link #shareQR()}.
     */
    private void shareLink() {
        shareQR(); // Same logic for now
    }

    /**
     * Navigates to the waitlist management screen ({@code viewEntrantsFragment}), passing the
     * current event ID and name as arguments.
     */
    private void openWaitlistManager() {
        Bundle args = new Bundle();
        args.putString("eventId", eventId);

        String eventName = getText(editName);
        if (!eventName.isEmpty()) {
            args.putString("eventName", eventName);
        }

        Navigation.findNavController(requireView()).navigate(R.id.viewEntrantsFragment, args);
    }

    /**
     * Navigates to the enrolled-entrants management screen ({@code enrolledFragment}), passing
     * the current event ID as an argument.
     */
    private void openEnrolledManager() {
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        Navigation.findNavController(requireView()).navigate(R.id.enrolledFragment, args);
    }

    /**
     * Reads all input fields, validates them, then delegates to
     * {@link #savePosterLocallyAndSave(Map)} to persist the changes to Firestore.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>Name, capacity, event date, and both registration dates are required.</li>
     *   <li>Capacity and sample-size must parse as integers.</li>
     *   <li>Registration period must be chronologically valid ({@link #isValidRegistrationPeriod}).</li>
     * </ul>
     */
    private void saveEventChanges() {
        String name = getText(editName);
        String description = getText(editDescription);
        String capStr = getText(editCapacity);
        String samStr = getText(editSampleSize);
        String eDate = getText(editEventDate);
        String rStart = getText(editRegistrationStart);
        String rEnd = getText(editRegistrationEnd);

        if (name.isEmpty() || capStr.isEmpty() || eDate.isEmpty() || rStart.isEmpty() || rEnd.isEmpty()) {
            TigerToast.show(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT);
            return;
        }

        int capacity, sampleSize;
        try {
            capacity = Integer.parseInt(capStr);
            sampleSize = Integer.parseInt(samStr);
        } catch (NumberFormatException e) {
            TigerToast.show(requireContext(), "Invalid numbers", Toast.LENGTH_SHORT);
            return;
        }

        if (!isValidRegistrationPeriod(eDate, rStart, rEnd)) return;

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);
        data.put("amount", capacity);
        data.put("sampleSize", sampleSize);
        data.put("event_date", eDate);
        data.put("registration_start", rStart);
        data.put("registration_end", rEnd);
        String location = editLocation != null && editLocation.getText() != null
                ? editLocation.getText().toString().trim() : "";
        data.put("location", location);
        data.put("isPrivate", isPrivateEvent);
        data.put("geolocationRequired", switchGeolocationRequired != null && switchGeolocationRequired.isChecked());

        savePosterLocallyAndSave(data);
    }

    /**
     * Handles three poster scenarios before calling {@link #performFirestoreUpdate(Map)}:
     * <ol>
     *   <li><b>Removed:</b> deletes the existing poster from Firebase Storage and clears
     *       {@code posterUrl} in {@code data}.</li>
     *   <li><b>New image selected:</b> uploads {@link #posterUri} to Storage, sets the
     *       {@code posterUrl} in {@code data} to the download URL.</li>
     *   <li><b>Unchanged:</b> proceeds directly to the Firestore update.</li>
     * </ol>
     *
     * @param data The event field map to persist; the {@code posterUrl} key may be mutated
     *             depending on the poster scenario.
     */
    private void savePosterLocallyAndSave(Map<String, Object> data) {
        if (posterRemoved) {
            // Delete the old poster from Firebase Storage
            StorageReference oldRef = FirebaseStorage.getInstance().getReference()
                    .child("posters/" + eventId + ".jpg");
            oldRef.delete().addOnFailureListener(e ->
                    Log.w(TAG, "No existing poster to delete or delete failed", e));
            data.put("posterUrl", "");
            performFirestoreUpdate(data);
            return;
        }

        if (posterUri == null) {
            performFirestoreUpdate(data);
            return;
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("posters/" + eventId + ".jpg");
        storageRef.putFile(posterUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            data.put("posterUrl", downloadUri.toString());
                            performFirestoreUpdate(data);
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL", e);
                            TigerToast.show(requireContext(), "Failed to upload poster", Toast.LENGTH_SHORT);
                            performFirestoreUpdate(data);
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload poster", e);
                    if (isAdded()) {
                        TigerToast.show(requireContext(), "Failed to upload poster", Toast.LENGTH_SHORT);
                    }
                    performFirestoreUpdate(data);
                });
    }

    /**
     * Merges the given field map into the Firestore event document via an {@code update} call,
     * then pops the back-stack on success or shows an error toast on failure.
     *
     * @param data The map of field names to new values to write to Firestore.
     */
    private void performFirestoreUpdate(Map<String, Object> data) {
        db.collection("events").document(eventId).update(data)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    TigerToast.show(requireContext(), "Event updated", Toast.LENGTH_SHORT);
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    TigerToast.show(requireContext(), "Update failed", Toast.LENGTH_SHORT);
                });
    }

    /**
     * Validates that the registration window is chronologically sound:
     * registration start must not be after registration end, and registration end must not be
     * after the event date.  Displays a toast describing the specific violation if invalid.
     *
     * @param eDate  Event date string in {@code yyyy-MM-dd} format.
     * @param rStart Registration start date string in {@code yyyy-MM-dd} format.
     * @param rEnd   Registration end date string in {@code yyyy-MM-dd} format.
     * @return {@code true} if all dates are parseable and the period is valid.
     */
    private boolean isValidRegistrationPeriod(String eDate, String rStart, String rEnd) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        try {
            Date event = sdf.parse(eDate);
            Date start = sdf.parse(rStart);
            Date end = sdf.parse(rEnd);
            if (start.after(end)) {
                TigerToast.show(requireContext(), "Start must be before end", Toast.LENGTH_SHORT);
                return false;
            }
            if (end.after(event)) {
                TigerToast.show(requireContext(), "Registration must end before event", Toast.LENGTH_SHORT);
                return false;
            }
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Safely extracts the trimmed text from a {@link TextInputEditText}.
     *
     * @param et The input field to read; must not be {@code null}.
     * @return The trimmed text, or an empty string if the editable content is {@code null}.
     */
    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    /**
     * Shows a confirmation {@link android.app.AlertDialog} warning the organizer that deletion is
     * irreversible, then calls {@link #deleteEvent()} if confirmed.
     */
    private void confirmDeleteEvent() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the event from Firestore and attempts to remove the associated poster from Firebase
     * Storage (a missing poster is treated as a no-op so the Firestore deletion still proceeds).
     * Navigates back on success and shows an error toast on failure.
     */
    private void deleteEvent() {
        StorageReference posterRef = FirebaseStorage.getInstance().getReference()
                .child("posters/" + eventId + ".jpg");
        posterRef.delete().addOnCompleteListener(task -> {
            // Proceed with Firestore deletion whether or not the poster existed
            db.collection("events").document(eventId).delete()
                    .addOnSuccessListener(aVoid -> {
                        if (!isAdded()) return;
                        TigerToast.show(requireContext(), "Event deleted", Toast.LENGTH_SHORT);
                        Navigation.findNavController(requireView()).popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        TigerToast.show(requireContext(), "Failed to delete event", Toast.LENGTH_SHORT);
                    });
        });
    }
}
