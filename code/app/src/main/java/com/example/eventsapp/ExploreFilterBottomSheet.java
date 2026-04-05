package com.example.eventsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Bottom sheet dialog for applying filters on the Explore page.
 * Supports "Show Available Only" toggle and date range selection.
 */
public class ExploreFilterBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_AVAILABLE_ONLY = "availableOnly";
    private static final String ARG_DATE_FROM = "dateFrom";
    private static final String ARG_DATE_TO = "dateTo";

    private MaterialSwitch switchAvailableOnly;
    private TextView tvDateFrom;
    private TextView tvDateTo;
    private String dateFrom;
    private String dateTo;

    private OnFilterAppliedListener listener;

    /**
     * Callback interface invoked when the user confirms their filter selections.
     */
    public interface OnFilterAppliedListener {
        /**
         * Called with the chosen filter values when the user taps "Apply Filters".
         *
         * @param availableOnly {@code true} if only events with open spots should be shown.
         * @param dateFrom      Lower bound of the date range filter ({@code yyyy-MM-dd}),
         *                      or {@code null} if not set.
         * @param dateTo        Upper bound of the date range filter ({@code yyyy-MM-dd}),
         *                      or {@code null} if not set.
         */
        void onFiltersApplied(boolean availableOnly, String dateFrom, String dateTo);
    }

    /**
     * Creates a new instance with the current filter state pre-populated so that
     * re-opening the sheet reflects any previously chosen filters.
     *
     * @param availableOnly Whether the "Available Only" toggle was active.
     * @param dateFrom      The previously selected start date ({@code yyyy-MM-dd}), or {@code null}.
     * @param dateTo        The previously selected end date ({@code yyyy-MM-dd}), or {@code null}.
     * @return A new {@link ExploreFilterBottomSheet} with its arguments set.
     */
    public static ExploreFilterBottomSheet newInstance(boolean availableOnly, String dateFrom, String dateTo) {
        ExploreFilterBottomSheet sheet = new ExploreFilterBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_AVAILABLE_ONLY, availableOnly);
        args.putString(ARG_DATE_FROM, dateFrom);
        args.putString(ARG_DATE_TO, dateTo);
        sheet.setArguments(args);
        return sheet;
    }

    /**
     * Registers the listener to be invoked when the user confirms their filter selections.
     * Must be called before the sheet is shown.
     *
     * @param listener The callback to notify with the chosen filter values.
     */
    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    /**
     * Inflates the bottom sheet layout.
     *
     * @param inflater           The LayoutInflater object to inflate views.
     * @param container          The parent ViewGroup that the fragment's UI will be attached to.
     * @param savedInstanceState Previously saved state (unused here).
     * @return The inflated root view for the bottom sheet.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore_filter_bottom_sheet, container, false);
    }

    /**
     * Initialises UI components, restores prior filter state from the fragment arguments,
     * and wires up click listeners for the date-picker cards, the Clear button, and
     * the Apply button.
     *
     * @param view               The inflated bottom sheet view.
     * @param savedInstanceState If non-null, the fragment is being re-created from saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchAvailableOnly = view.findViewById(R.id.switchAvailableOnly);
        tvDateFrom = view.findViewById(R.id.tvDateFrom);
        tvDateTo = view.findViewById(R.id.tvDateTo);
        MaterialCardView cardDateFrom = view.findViewById(R.id.cardDateFrom);
        MaterialCardView cardDateTo = view.findViewById(R.id.cardDateTo);
        MaterialButton btnClear = view.findViewById(R.id.btnClearFilters);
        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilters);

        // Restore state from arguments
        Bundle args = getArguments();
        if (args != null) {
            switchAvailableOnly.setChecked(args.getBoolean(ARG_AVAILABLE_ONLY, false));
            dateFrom = args.getString(ARG_DATE_FROM);
            dateTo = args.getString(ARG_DATE_TO);
            if (dateFrom != null && !dateFrom.isEmpty()) {
                tvDateFrom.setText(formatForDisplay(dateFrom));
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                tvDateTo.setText(formatForDisplay(dateTo));
            }
        }

        cardDateFrom.setOnClickListener(v -> showDatePicker(true));
        cardDateTo.setOnClickListener(v -> showDatePicker(false));

        btnClear.setOnClickListener(v -> {
            switchAvailableOnly.setChecked(false);
            dateFrom = null;
            dateTo = null;
            tvDateFrom.setText("");
            tvDateTo.setText("");
        });

        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFiltersApplied(switchAvailableOnly.isChecked(), dateFrom, dateTo);
            }
            dismiss();
        });
    }

    /**
     * Opens a {@link MaterialDatePicker} and stores the selected date in either
     * {@link #dateFrom} or {@link #dateTo}, updating the corresponding label TextView.
     * The date is stored in {@code yyyy-MM-dd} format (UTC) for consistent comparison.
     *
     * @param isFrom {@code true} to set the "from" (start) date; {@code false} for the "to" (end) date.
     */
    private void showDatePicker(boolean isFrom) {
        String title = isFrom ? "From Date" : "To Date";
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String formatted = sdf.format(new Date(selection));

            if (isFrom) {
                dateFrom = formatted;
                tvDateFrom.setText(formatForDisplay(formatted));
            } else {
                dateTo = formatted;
                tvDateTo.setText(formatForDisplay(formatted));
            }
        });

        picker.show(getChildFragmentManager(), isFrom ? "date_from" : "date_to");
    }

    /**
     * Converts a {@code yyyy-MM-dd} date string to a user-friendly format like "April 2, 2026"
     * suitable for display in the date label TextViews.
     *
     * @param dateStr The ISO date string to format.
     * @return The formatted display string, or {@code dateStr} unchanged if parsing fails.
     */
    private String formatForDisplay(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
            SimpleDateFormat output = new SimpleDateFormat("MMMM d, yyyy", Locale.CANADA);
            Date date = input.parse(dateStr);
            return date != null ? output.format(date) : dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }
}
