package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Fragment that displays the final list of entrants enrolled in an event.
 *
 * Fulfills US 02.06.03 - As an organizer I want to see a final list of entrants
 * who enrolled for the event.
 */
public class EnrolledFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";

    private MaterialToolbar toolbarEnrolled;
    private RecyclerView rvEnrolled;
    private TextView tvEnrolledStats;
    private TextInputEditText etSearchEnrolled;
    private MaterialButton btnExportCsv;
    private MaterialButton btnSeeCancelled;

    private FirebaseFirestore db;
    private CollectionReference enrolledRef;

    private final ArrayList<EnrolledEntrant> allEnrolledEntrants = new ArrayList<>();
    private final ArrayList<EnrolledEntrant> filteredEnrolledEntrants = new ArrayList<>();
    private EnrolledEntrantAdapter adapter;

    private String eventId;

    public EnrolledFragment() {
        super(R.layout.view_enrolled);
    }

    public static EnrolledFragment newInstance(String eventId) {
        EnrolledFragment fragment = new EnrolledFragment();
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
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbarEnrolled = view.findViewById(R.id.toolbar_enrolled);
        rvEnrolled = view.findViewById(R.id.rv_enrolled);
        tvEnrolledStats = view.findViewById(R.id.tv_enrolled_stats);
        etSearchEnrolled = view.findViewById(R.id.et_search_enrolled);
        btnExportCsv = view.findViewById(R.id.btn_export_csv);
        btnSeeCancelled = view.findViewById(R.id.btn_see_cancelled);

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "No event selected", Toast.LENGTH_SHORT).show();
            return;
        }

        db = FirebaseFirestore.getInstance();
        enrolledRef = db.collection("events")
                .document(eventId)
                .collection("enrolled");

        adapter = new EnrolledEntrantAdapter(requireContext(), filteredEnrolledEntrants);
        rvEnrolled.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEnrolled.setAdapter(adapter);

        toolbarEnrolled.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        etSearchEnrolled.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntrants(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnExportCsv.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Export CSV", Toast.LENGTH_SHORT).show());
        btnSeeCancelled.setOnClickListener(v -> openCancelledFragment());

        loadEnrolledEntrants();
    }

    private void loadEnrolledEntrants() {
        enrolledRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Failed to load enrolled entrants", Toast.LENGTH_SHORT).show();
                return;
            }

            allEnrolledEntrants.clear();

            if (value != null) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String userId = snapshot.getId();
                    String name = snapshot.getString("name");
                    String email = snapshot.getString("email");
                    String status = snapshot.getString("status");

                    if (name == null) {
                        name = "Unknown User";
                    }
                    if (email == null) {
                        email = "No email";
                    }
                    if (status == null) {
                        status = "Enrolled";
                    }

                    allEnrolledEntrants.add(new EnrolledEntrant(userId, name, email, status));
                }
            }

            String query = etSearchEnrolled.getText() != null ? etSearchEnrolled.getText().toString() : "";
            filterEntrants(query);
        });
    }

    private void filterEntrants(@NonNull String query) {
        filteredEnrolledEntrants.clear();
        String normalizedQuery = query.trim().toLowerCase(Locale.CANADA);

        for (EnrolledEntrant entrant : allEnrolledEntrants) {
            if (normalizedQuery.isEmpty()
                    || containsIgnoreCase(entrant.getName(), normalizedQuery)
                    || containsIgnoreCase(entrant.getEmail(), normalizedQuery)) {
                filteredEnrolledEntrants.add(entrant);
            }
        }

        adapter.notifyDataSetChanged();
        tvEnrolledStats.setText(normalizedQuery.isEmpty()
                ? "Total Enrolled: " + allEnrolledEntrants.size()
                : "Showing " + filteredEnrolledEntrants.size() + " of " + allEnrolledEntrants.size());
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.CANADA).contains(query);
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
