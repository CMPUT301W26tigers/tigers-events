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
 * - Toggling event privacy status.
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
    private ImageView ivPoster;
    private ImageView ivQR;
    private TextView tvEventLink;
    private MaterialButton btnTogglePrivateEvent;
    private MaterialSwitch switchGeolocationRequired;
    private View shareTitle;
    private MaterialButton btnViewWaitlist;
    private MaterialButton btnManageEnrolledList;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (eventId == null) {
            Toast.makeText(requireContext(), "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        initializeViews(view);
        loadEventData();
        setupToolbar(view);
    }

    private void initializeViews(View view) {
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
        btnTogglePrivateEvent = view.findViewById(R.id.btn_toggle_private_event);
        switchGeolocationRequired = view.findViewById(R.id.switch_geolocation_required);
        shareTitle = view.findViewById(R.id.tv_share);
        btnViewWaitlist = view.findViewById(R.id.btn_view_waitlist);
        btnManageEnrolledList = view.findViewById(R.id.btn_manage_enrolled_list);

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
        view.findViewById(R.id.btn_back).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        btnTogglePrivateEvent.setOnClickListener(v -> {
            isPrivateEvent = !isPrivateEvent;
            updatePrivateUi();
        });

        if (btnViewWaitlist != null) {
            btnViewWaitlist.setOnClickListener(v -> openWaitlistManager());
        }
        if (btnManageEnrolledList != null) {
            btnManageEnrolledList.setOnClickListener(v -> openEnrolledManager());
        }

        view.findViewById(R.id.btn_share_qr).setOnClickListener(v -> shareQR());
        view.findViewById(R.id.btn_share_link).setOnClickListener(v -> shareLink());
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    Navigation.findNavController(view).popBackStack()
            );
        }
    }

    private void setupDatePickers() {
        editEventDate.setOnClickListener(v -> showDatePicker(editEventDate));
        editRegistrationStart.setOnClickListener(v -> showDatePicker(editRegistrationStart));
        editRegistrationEnd.setOnClickListener(v -> showDatePicker(editRegistrationEnd));
        
        editEventDate.setFocusable(false);
        editRegistrationStart.setFocusable(false);
        editRegistrationEnd.setFocusable(false);
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            String date = String.format(Locale.CANADA, "%04d-%02d-%02d", year, month + 1, day);
            target.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

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

    private void updatePrivateUi() {
        btnTogglePrivateEvent.setText(isPrivateEvent ? "Set Event Public" : "Set Event Private");
        
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

    private void shareQR() {
        String link = "tigers-events://event/" + eventId;
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, link);
        startActivity(android.content.Intent.createChooser(shareIntent, "Share QR link"));
    }

    private void shareLink() {
        shareQR(); // Same logic for now
    }

    private void openWaitlistManager() {
        Bundle args = new Bundle();
        args.putString("eventId", eventId);

        String eventName = getText(editName);
        if (!eventName.isEmpty()) {
            args.putString("eventName", eventName);
        }

        Navigation.findNavController(requireView()).navigate(R.id.viewEntrantsFragment, args);
    }

    private void openEnrolledManager() {
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        Navigation.findNavController(requireView()).navigate(R.id.enrolledFragment, args);
    }

    private void saveEventChanges() {
        String name = getText(editName);
        String description = getText(editDescription);
        String capStr = getText(editCapacity);
        String samStr = getText(editSampleSize);
        String eDate = getText(editEventDate);
        String rStart = getText(editRegistrationStart);
        String rEnd = getText(editRegistrationEnd);

        if (name.isEmpty() || capStr.isEmpty() || eDate.isEmpty() || rStart.isEmpty() || rEnd.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity, sampleSize;
        try {
            capacity = Integer.parseInt(capStr);
            sampleSize = Integer.parseInt(samStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid numbers", Toast.LENGTH_SHORT).show();
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
        data.put("isPrivate", isPrivateEvent);
        data.put("geolocationRequired", switchGeolocationRequired != null && switchGeolocationRequired.isChecked());

        savePosterLocallyAndSave(data);
    }

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
                            Toast.makeText(requireContext(), "Failed to upload poster", Toast.LENGTH_SHORT).show();
                            performFirestoreUpdate(data);
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload poster", e);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to upload poster", Toast.LENGTH_SHORT).show();
                    }
                    performFirestoreUpdate(data);
                });
    }

    private void performFirestoreUpdate(Map<String, Object> data) {
        db.collection("events").document(eventId).update(data)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Event updated", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isValidRegistrationPeriod(String eDate, String rStart, String rEnd) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        try {
            Date event = sdf.parse(eDate);
            Date start = sdf.parse(rStart);
            Date end = sdf.parse(rEnd);
            if (start.after(end)) {
                Toast.makeText(requireContext(), "Start must be before end", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (end.after(event)) {
                Toast.makeText(requireContext(), "Registration must end before event", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
