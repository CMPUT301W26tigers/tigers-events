package com.example.eventsapp;

import android.app.DatePickerDialog;
import android.content.Context;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private TextInputEditText editName;
    private TextInputEditText editDescription;
    private TextInputEditText editCapacity;
    private TextInputEditText editSampleSize;
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
    private MaterialButton btnViewWaitlist;
    private boolean isPrivateEvent;
    private MaterialSwitch switchGeolocationRequired;

    private Uri posterUri;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_event, container, false);
    }

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
        btnViewWaitlist = view.findViewById(R.id.btn_view_waitlist);
        switchGeolocationRequired = view.findViewById(R.id.switch_geolocation_required);
        setupDatePickers();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        int[] sectionHeaderIds = {
                R.id.tv_section_name, R.id.tv_section_description,
                R.id.tv_section_logistics, R.id.tv_share
        };
        for (int id : sectionHeaderIds) {
            TextView tv = view.findViewById(id);
            if (tv != null) tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

        MaterialButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        Event event = new Event(null, "", 1, "", "", "", "", "",  0);
        event.setId(java.util.UUID.randomUUID().toString());
        updateQRCode(event);

        android.widget.TextView tvEventLink = view.findViewById(R.id.tv_event_link);
        tvEventLink.setText(event.getEventDeepLink());
        updatePrivateEventUi(event);
        btnTogglePrivateEvent.setOnClickListener(v -> {
            isPrivateEvent = !isPrivateEvent;
            updatePrivateEventUi(event);
        });

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

        view.findViewById(R.id.btn_edit_poster).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        ivPoster.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnViewWaitlist.setOnClickListener(v -> saveAndNavigateToWaitlist(event));

        view.findViewById(R.id.btn_done).setOnClickListener(v -> saveAndGoBack(event));

        view.findViewById(R.id.btn_share_qr).setOnClickListener(v -> {
            String deepLink = event.getEventDeepLink();
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, deepLink);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share QR link"));
        });
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

    private void updateQRCode(Event event) {
        String link = event.getEventDeepLink();
        Bitmap qrBitmap = QRCodeUtil.generateQRCode(link, QR_SIZE, QR_SIZE);
        if (qrBitmap != null && ivQR != null) {
            ivQR.setImageBitmap(qrBitmap);
        }
    }

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
        if (btnViewWaitlist != null) {
            btnViewWaitlist.setText("Manage Waitlist");
        }
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

    private void saveAndNavigateToWaitlist(Event event) {
        if (prepareEventData(event)) {
            Users currentUser = UserManager.getInstance().getCurrentUser();
            Map<String, Object> data = createEventMap(event, currentUser);

            savePosterLocallyAndSave(event, data, currentUser, () -> {
                if (!isAdded()) return;
                Bundle args = new Bundle();
                args.putString("eventId", event.getId());
                args.putString("eventName", event.getName());
                Navigation.findNavController(requireView()).navigate(R.id.viewEntrantsFragment, args);
            });
        }
    }

    private void saveAndGoBack(Event event) {
        if (prepareEventData(event)) {
            Users currentUser = UserManager.getInstance().getCurrentUser();
            Map<String, Object> data = createEventMap(event, currentUser);

            savePosterLocallyAndSave(event, data, currentUser, () -> {
                if (!isAdded()) return;
                Navigation.findNavController(requireView()).popBackStack();
            });
        }
    }

    private boolean prepareEventData(Event event) {
        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
        String capacityStr = editCapacity.getText() != null ? editCapacity.getText().toString().trim() : "1";
        String sampleStr = editSampleSize != null && editSampleSize.getText() != null
                ? editSampleSize.getText().toString().trim() : "0";

        String eventDate = getText(editEventDate);
        String registrationStart = getText(editRegistrationStart);
        String registrationEnd = getText(editRegistrationEnd);

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Event name required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (capacityStr.isEmpty()) {
            Toast.makeText(requireContext(), "Capacity required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (eventDate.isEmpty()) {
            editEventDate.setError("Event date required");
            return false;
        }
        if (registrationStart.isEmpty()) {
            editRegistrationStart.setError("Registration start required");
            return false;
        }
        if (registrationEnd.isEmpty()) {
            editRegistrationEnd.setError("Registration end required");
            return false;
        }

        int capacity;
        int sampleSize;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                Toast.makeText(requireContext(), "Capacity must be positive", Toast.LENGTH_SHORT).show();
                return false;
            }
            sampleSize = Integer.parseInt(sampleStr);
            if (sampleSize < 0) sampleSize = 0;
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid capacity", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidRegistrationPeriod(eventDate, registrationStart, registrationEnd)) {
            return false;
        }

        event.setName(name);
        event.setDescription(description);
        event.setAmount(capacity);
        event.setSampleSize(sampleSize);
        event.setEvent_date(eventDate);
        event.setRegistration_start(registrationStart);
        event.setRegistration_end(registrationEnd);
        return true;
    }

    private Map<String, Object> createEventMap(Event event, Users currentUser) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", event.getId());
        data.put("name", event.getName());
        data.put("amount", event.getAmount());
        data.put("description", event.getDescription());
        data.put("sampleSize", event.getSampleSize());
        data.put("event_date", event.getEvent_date());
        data.put("registration_start", event.getRegistration_start());
        data.put("registration_end", event.getRegistration_end());
        data.put("isPrivate", isPrivateEvent);
        data.put("coOrganizerIds", new ArrayList<String>());
        data.put("pendingCoOrganizerIds", new ArrayList<String>());
        data.put("geolocationRequired", switchGeolocationRequired != null && switchGeolocationRequired.isChecked());

        if (currentUser != null && currentUser.getId() != null) {
            data.put("createdBy", currentUser.getId());
        }
        return data;
    }

    private void savePosterLocallyAndSave(Event event, Map<String, Object> data, @Nullable Users currentUser,
                                         @NonNull Runnable onSuccess) {
        if (posterUri == null) {
            data.put("posterUrl", "");
            saveEventToFirestore(event, data, currentUser, onSuccess);
            return;
        }

        try {
            String fileName = "poster_" + event.getId() + ".jpg";
            File file = new File(requireContext().getFilesDir(), fileName);
            try (InputStream in = requireContext().getContentResolver().openInputStream(posterUri);
                 OutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            String localPath = file.getAbsolutePath();
            data.put("posterUrl", localPath);
            event.setPosterUrl(localPath);
            saveEventToFirestore(event, data, currentUser, onSuccess);
        } catch (IOException e) {
            Log.e("CreateEvent", "Failed to save poster locally", e);
            Toast.makeText(requireContext(), "Failed to save poster", Toast.LENGTH_SHORT).show();
            data.put("posterUrl", "");
            saveEventToFirestore(event, data, currentUser, onSuccess);
        }
    }

    private void saveEventToFirestore(Event event, Map<String, Object> data, @Nullable Users currentUser,
                                      @NonNull Runnable onSuccess) {
        DocumentReference docRef = db.collection("events").document(event.getId());
        docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Log.d("CreateEvent", "Event saved");
                    Toast.makeText(requireContext(), "Event created", Toast.LENGTH_SHORT).show();
                    if (currentUser != null && currentUser.getId() != null) {
                        EventCleanupHelper.writeHistoryRecord(currentUser.getId(), event.getId(), data, "ORGANIZED");
                    }
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e("CreateEvent", "Failed to save", e);
                    Toast.makeText(requireContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
                });
    }

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

    private String getText(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }
}
