package com.example.eventsapp;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AdminManageEventsFragment extends Fragment {

    public AdminManageEventsFragment() {
        super(R.layout.fragment_admin_manage_events);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBackToUser).setOnClickListener(v -> {
            requireActivity().finish();
        });
    }
}
