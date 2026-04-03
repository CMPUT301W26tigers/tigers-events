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
     * Callback interface for when the user applies filters.
     */
    public interface OnFilterAppliedListener {
        void onFiltersApplied(boolean availableOnly, String dateFrom, String dateTo);
    }

    /**
     * Creates a new instance with the current filter state preserved.
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

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore_filter_bottom_sheet, container, false);
    }

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
     * Converts "yyyy-MM-dd" to a user-friendly format like "April 2, 2026".
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
