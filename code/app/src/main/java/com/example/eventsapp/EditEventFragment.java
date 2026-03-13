/**
 THIS CLASS IS OBSOLETE NOW, IMPORTANT FUNCTIONALITY HAS BEEN MOVED TO CreateEventFragment
package com.example.eventsapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

/**
 * A fragment that allows an organizer to edit the details of an existing event.
 * Currently focuses on managing event dates and registration periods, and
 * providing navigation to the waitlist and enrolled entrants views.
 */
public class EditEventFragment extends Fragment {

    private EditText editEventDate;
    private EditText editRegistrationStart;
    private EditText editRegistrationEnd;
    private MaterialButton btnViewWaitlist;
    private MaterialButton btnViewEnrolled;

    /**
     * Default constructor for EditEventFragment.
     * Uses the layout R.layout.edit_event.
     */
    public EditEventFragment() {
        super(R.layout.edit_event);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned. Initializes views, date pickers, and button listeners.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupDatePickers();
        setupButtons();
    }

    /**
     * Binds UI components from the layout to class variables.
     *
     * @param view The root view of the fragment.
     */
    private void bindViews(@NonNull View view) {

        editEventDate = view.findViewById(R.id.edit_event_date);
        editRegistrationStart = view.findViewById(R.id.edit_registration_start);
        editRegistrationEnd = view.findViewById(R.id.edit_registration_end);
        btnViewWaitlist = view.findViewById(R.id.btn_view_waitlist);
        btnViewEnrolled = view.findViewById(R.id.btn_view_enrolled);
    }

    /**
     * Sets up click listeners for the buttons in the fragment.
     */
    private void setupButtons() {
        btnViewWaitlist.setOnClickListener(v -> {
            if (validateFields()) {
                openWaitlistFragment();
            }
        });
        btnViewEnrolled.setOnClickListener(v -> {
            if (validateFields()) {
                openEnrolledFragment();
            }
        });
    }

    /**
     * Configures EditText fields to show a {@link DatePickerDialog} when clicked.
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
     * Displays a {@link DatePickerDialog} and updates the target EditText with the selected date.
     *
     * @param target The EditText to be updated with the selected date string.
     */
    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    target.setText(date);
                    }, year, month, day);

        picker.show();
    }

    /**
     * Validates that the date fields are not empty and that the dates are logically consistent
     * (e.g., registration ends before the event starts).
     *
     * @return True if all fields are valid, false otherwise.
     */
    private boolean validateFields() {
        editEventDate.setError(null);
        editRegistrationStart.setError(null);
        editRegistrationEnd.setError(null);
        String eventDateStr = editEventDate.getText().toString().trim();
        String registrationStartStr = editRegistrationStart.getText().toString().trim();
        String registrationEndStr = editRegistrationEnd.getText().toString().trim();


        if (TextUtils.isEmpty(eventDateStr)) {
            editEventDate.setError("Event date is required");
            return false;
        }

        if (TextUtils.isEmpty(registrationStartStr)) {
            editRegistrationStart.setError("Registration start is required");
            return false;
        }

        if (TextUtils.isEmpty(registrationEndStr)) {
            editRegistrationEnd.setError("Registration end is required");
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
        sdf.setLenient(false);

        try {
            Date eventDate = sdf.parse(eventDateStr);
            Date registrationStart = sdf.parse(registrationStartStr);
            Date registrationEnd = sdf.parse(registrationEndStr);

            if (registrationStart == null || registrationEnd == null || eventDate == null) {
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
     * Navigates to the {@link WaitlistFragment} for the current event.
     */
    private void openWaitlistFragment() {
        String currentEventId = "EventId";

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, WaitlistFragment.newInstance(currentEventId))
                .addToBackStack(null)
                .commit();
    }

    /**
     * Navigates to the {@link EnrolledFragment} for the current event.
     */
    private void openEnrolledFragment() {
        String currentEventId = event.getId();

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, EnrolledFragment.newInstance(currentEventId))
                .addToBackStack(null)
                .commit();
    }

}
 */
