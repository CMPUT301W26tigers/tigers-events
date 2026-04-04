package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A fragment that displays the list of events that have been cancelled for the user.
 */
public class OrganizerCancelledFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";
    private String eventId;

    private MaterialToolbar toolbarCancelled;
    private RecyclerView rvCancelled;
    private TextView tvCancelledStats;
    private TextInputEditText etSearchCancelled;

    private FirebaseFirestore db;
    private CollectionReference waitlistRef;

    private final ArrayList<Entrant> allCancelledEntrants = new ArrayList<>();
    private final ArrayList<Entrant> filteredCancelledEntrants = new ArrayList<>();
    private CancelledEntrantAdapter adapter;

    /**
     * Default constructor for OrganizerCancelledFragment.
     * Uses the layout R.layout.view_cancelled.
     */
    public OrganizerCancelledFragment() {
        super(R.layout.fragment_organizer_cancelled);
    }

    public static OrganizerCancelledFragment newInstance(String eventId) {
        OrganizerCancelledFragment fragment = new OrganizerCancelledFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        toolbarCancelled = view.findViewById(R.id.toolbar_cancelled);
        rvCancelled = view.findViewById(R.id.rv_cancelled);
        tvCancelledStats = view.findViewById(R.id.tv_cancelled_stats);
        etSearchCancelled = view.findViewById(R.id.et_search_cancelled);

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "No event selected", Toast.LENGTH_SHORT).show();
            return;
        }

        db = FirebaseFirestore.getInstance();
        // Accessing the same waitlist collection where entrants initially applied
        waitlistRef = db.collection("events").document(eventId).collection("entrants");

        adapter = new CancelledEntrantAdapter(requireContext(), filteredCancelledEntrants);
        rvCancelled.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCancelled.setAdapter(adapter);

        toolbarCancelled.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        etSearchCancelled.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntrants(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadCancelledEntrants();


    }

    private void loadCancelledEntrants() {
        waitlistRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load cancelled entrants", Toast.LENGTH_SHORT).show();
                }
                return;
            }


            allCancelledEntrants.clear();

            if (value != null) {
                for (QueryDocumentSnapshot snapshot : value) {
                    Entrant entrant = snapshot.toObject(Entrant.class);
                    entrant.setId(snapshot.getId()); // ensure we have ID stored

                    // Only show entrants with DECLINED or CANCELLED status
                    if (entrant.getStatus() == Entrant.Status.DECLINED || entrant.getStatus() == Entrant.Status.CANCELLED) {
                        allCancelledEntrants.add(entrant);
                    }
                }
            }

            String query = etSearchCancelled.getText() != null ? etSearchCancelled.getText().toString() : "";
            filterEntrants(query);
        });


    }

    private void filterEntrants(@NonNull String query) {
        filteredCancelledEntrants.clear();
        String normalizedQuery = query.trim().toLowerCase(Locale.CANADA);


        for (Entrant entrant : allCancelledEntrants) {
            if (normalizedQuery.isEmpty()
                    || containsIgnoreCase(entrant.getName(), normalizedQuery)
                    || containsIgnoreCase(entrant.getEmail(), normalizedQuery)) {
                filteredCancelledEntrants.add(entrant);
            }
        }

        adapter.notifyDataSetChanged();
        tvCancelledStats.setText(normalizedQuery.isEmpty()
                ? "Total Cancelled: " + allCancelledEntrants.size()
                : "Showing " + filteredCancelledEntrants.size() + " of " + allCancelledEntrants.size());


    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.CANADA).contains(query);
    }
}