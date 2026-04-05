package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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
 * Fragment that displays the final list of entrants enrolled (ACCEPTED) in an event.
 *
 * <p>Fulfills US 02.06.03 – As an organizer I want to see a final list of entrants
 * who enrolled for the event.
 *
 * <p>Features:
 * <ul>
 *   <li>Real-time synchronisation with the {@code enrolled} sub-collection.</li>
 *   <li>Name/email search filter.</li>
 *   <li>CSV export via the system document-creator contract.</li>
 *   <li>Navigation to the cancelled-entrants screen.</li>
 * </ul>
 */
public class OrganizerEnrolledFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";

    private MaterialToolbar toolbarEnrolled;
    private RecyclerView rvEnrolled;
    private TextView tvEnrolledStats;
    private TextInputEditText etSearchEnrolled;
    private MaterialButton btnExportCsv;
    private MaterialButton btnSeeCancelled;

    private FirebaseFirestore db;
    private CollectionReference enrolledRef;

    private final ArrayList<Entrant> allEnrolledEntrants = new ArrayList<>();
    private final ArrayList<Entrant> filteredEnrolledEntrants = new ArrayList<>();
    private EnrolledEntrantAdapter adapter;

    private String eventId;

    /**
     * Required public no-arg constructor. Supplies the layout resource so the
     * framework can recreate the fragment automatically.
     */
    public OrganizerEnrolledFragment() {
        super(R.layout.fragment_organizer_enrolled);
    }

    /**
     * Creates a new instance with the given event ID pre-loaded as an argument.
     *
     * @param eventId Firestore document ID of the target event
     * @return a configured {@link OrganizerEnrolledFragment}
     */
    public static OrganizerEnrolledFragment newInstance(String eventId) {
        OrganizerEnrolledFragment fragment = new OrganizerEnrolledFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<String> csvExportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    String[] headers = {"Name", "Email", "Status"};

                    boolean success = CSVExporter.exportToCsv(requireContext(), uri, headers, allEnrolledEntrants,
                            entrant -> new String[]{
                                    entrant.getName(),
                                    entrant.getEmail(),
                                    entrant.getStatus() != null ? entrant.getStatus().name() : "ACCEPTED"
                            });

                    if (success) {
                        Toast.makeText(requireContext(), "Enrolled list exported successfully!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to export enrolled list.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    /**
     * Reads the {@code eventId} from the fragment arguments so it is available
     * before the view is created.
     *
     * @param savedInstanceState previously saved state, or {@code null}
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    /**
     * Binds views, configures the adapter and search filter, wires up the CSV-export
     * and cancelled-list buttons, then starts listening for enrolled entrant changes.
     *
     * @param view               the root view provided by the framework
     * @param savedInstanceState previously saved state, or {@code null}
     */
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
            TigerToast.show(requireContext(), "No event selected", Toast.LENGTH_SHORT);
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

        btnExportCsv.setOnClickListener(v -> {
            if (allEnrolledEntrants.isEmpty()) {
                Toast.makeText(requireContext(), "No enrolled entrants to export", Toast.LENGTH_SHORT).show();
                return;
            }
            csvExportLauncher.launch("Enrolled_Export.csv");
        });

        btnSeeCancelled.setOnClickListener(v -> openCancelledFragment());

        loadEnrolledEntrants();
    }

    private void loadEnrolledEntrants() {
        enrolledRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                TigerToast.show(requireContext(), "Failed to load enrolled entrants", Toast.LENGTH_SHORT);
                return;
            }

            allEnrolledEntrants.clear();

            if (value != null) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String userId = snapshot.getId();
                    String name = snapshot.getString("name");
                    String email = snapshot.getString("email");
                    String statusStr = snapshot.getString("status");

                    if (name == null) name = "Unknown User";
                    if (email == null) email = "No email";

                    Entrant.Status statusEnum;
                    try {
                        statusEnum = (statusStr != null) ? Entrant.Status.valueOf(statusStr) : Entrant.Status.ACCEPTED;
                    } catch (IllegalArgumentException e) {
                        statusEnum = Entrant.Status.ACCEPTED;
                    }

                    Entrant entrant = new Entrant(userId, eventId, name, email, statusEnum);
                    entrant.setUserId(userId);
                    allEnrolledEntrants.add(entrant);
                }
            }

            String query = etSearchEnrolled.getText() != null ? etSearchEnrolled.getText().toString() : "";
            filterEntrants(query);
        });
    }

    private void filterEntrants(@NonNull String query) {
        filteredEnrolledEntrants.clear();
        String normalizedQuery = query.trim().toLowerCase(Locale.CANADA);

        for (Entrant entrant : allEnrolledEntrants) {
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
        Bundle bundle = new Bundle();
        bundle.putString("eventId", eventId);
        Navigation.findNavController(requireView()).navigate(R.id.action_enrolledFragment_to_cancelledFragment, bundle);
    }
}
