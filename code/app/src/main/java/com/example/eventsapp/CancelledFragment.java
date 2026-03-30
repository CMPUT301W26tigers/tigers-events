package com.example.eventsapp;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * A fragment that displays the list of events that have been cancelled for the user.
 */
public class CancelledFragment extends Fragment {

    /**
     * Default constructor for CancelledFragment.
     * Uses the layout R.layout.view_cancelled.
     */
    public CancelledFragment() {
        super(R.layout.view_cancelled);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_cancelled);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }
}
