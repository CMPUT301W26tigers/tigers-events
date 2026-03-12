package com.example.eventsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class WaitlistFragment extends Fragment {
    private MaterialToolbar toolbarWaitlist;
    private MaterialButton btnExportCsv;
    private MaterialButton btnSeeCancelled;

    public WaitlistFragment() {
        super(R.layout.view_waitlist);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbarWaitlist = view.findViewById(R.id.toolbar_waitlist);
        btnExportCsv = view.findViewById(R.id.btn_export_csv);
        btnSeeCancelled = view.findViewById(R.id.btn_see_cancelled);

        setupToolbar();
        setupButtons();
    }

    private void setupToolbar() {
        toolbarWaitlist.inflateMenu(R.menu.waitlist_menu);

        toolbarWaitlist.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        toolbarWaitlist.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_notify) {
                notifyChosenEntrants();
                return true;
            }
            return false;
        });
    }

    private void setupButtons() {
        btnExportCsv.setOnClickListener(v -> exportEntrantsAsCsv());

        btnSeeCancelled.setOnClickListener(v -> openCancelledFragment());
    }


    private void notifyChosenEntrants() {
        List<WaitlistEntrant> chosenEntrants = new ArrayList<>();

        for (WaitlistEntrant entrant : waitlistEntrants) {
            if (entrant.isChosen()) {
                chosenEntrants.add(entrant);
            }
        }

        if (chosenEntrants.isEmpty()) {
            Toast.makeText(requireContext(), "No chosen entrants to notify", Toast.LENGTH_SHORT).show();
            return;
        }

        for (WaitlistEntrant entrant : chosenEntrants) {
            NotificationItem notification = new NotificationItem(
                    "You've been selected!",
                    "You were chosen to sign up for " + eventName + ".", eventId,
                    "invitation",
                    false
            );

            db.collection("users")
                    .document(entrant.getUserId())
                    .collection("notifications")
                    .add(notification);
        }

        Toast.makeText(requireContext(), "Notifications sent to chosen entrants", Toast.LENGTH_SHORT).show();
    }

    private void exportEntrantsAsCsv() {
        Toast.makeText(requireContext(),
                "Export CSV clicked",
                Toast.LENGTH_SHORT).show();

        // add CSV here
    }

    private void openCancelledFragment() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, new CancelledFragment())
                .addToBackStack(null)
                .commit();
    }
}