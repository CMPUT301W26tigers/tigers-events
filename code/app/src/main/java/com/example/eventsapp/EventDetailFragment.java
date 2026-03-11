package com.example.eventsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Shows event description and poster when opened from QR code deep link.
 */
public class EventDetailFragment extends Fragment {

    private String eventId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
        } else {
            eventId = "";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvDescription = view.findViewById(R.id.tv_description);
        ImageView ivPoster = view.findViewById(R.id.iv_poster);

        if (eventId.isEmpty()) {
            tvName.setText("Event not found");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        tvName.setText(doc.getString("name"));
                        tvDescription.setText(doc.getString("description"));
                        // Poster URL could be loaded with Glide/Picasso - for now use placeholder
                    } else {
                        tvName.setText("Event not found");
                    }
                })
                .addOnFailureListener(e -> tvName.setText("Failed to load event"));
    }
}
