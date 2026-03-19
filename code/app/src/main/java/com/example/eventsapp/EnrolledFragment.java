package com.example.eventsapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 * Fragment that displays the final list of entrants enrolled in an event.
 *
 * Fulfills US 02.06.03 - As an organizer I want to see a final list of entrants
 * who enrolled for the event.
 *
 * The fragment retrieves enrolled users from the Firestore
 *
 * Each entrant is displayed using the EnrolledEntrantAdapter.
 */
public class EnrolledFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";

    private MaterialToolbar toolbarEnrolled;
    private RecyclerView rvEnrolled;
    private TextView tvEnrolledStats;
    private MaterialButton btnBackToEvent;

    private FirebaseFirestore db;
    private CollectionReference enrolledRef;

    private ArrayList<EnrolledEntrant> enrolledEntrants;
    private EnrolledEntrantAdapter adapter;

    private String eventId;

    /**
     * Default constructor for EnrolledFragment.
     * Uses the layout R.layout.view_enrolled.
     */
    public EnrolledFragment() {
        super(R.layout.view_enrolled);
    }

    /**
     * Creates a new instance of EnrolledFragment with the specified event ID.
     *
     * @param eventId The unique ID of the event to display enrolled entrants for.
     * @return A new instance of EnrolledFragment.
     */
    public static EnrolledFragment newInstance(String eventId) {
        EnrolledFragment fragment = new EnrolledFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called when the fragment is being created. Retrieves the event ID from the arguments.
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    /**
     * Initializes UI components and loads enrolled entrants from Firestore.
     *
     * Sets up RecyclerView and adapter, Firestore listener to retrieve enrolled entrants
     *
     * @param view fragment layout view
     * @param savedInstanceState previously saved state
     */
    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbarEnrolled = view.findViewById(R.id.toolbar_enrolled);
        rvEnrolled = view.findViewById(R.id.rv_enrolled);
        tvEnrolledStats = view.findViewById(R.id.tv_enrolled_stats);
        btnBackToEvent = view.findViewById(R.id.btn_back_to_event);

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "No event selected", Toast.LENGTH_SHORT).show();
            return;
        }

        db = FirebaseFirestore.getInstance();
        enrolledRef = db.collection("events")
                .document(eventId)
                .collection("enrolled");

        enrolledEntrants = new ArrayList<>();
        adapter = new EnrolledEntrantAdapter(requireContext(), enrolledEntrants);

        rvEnrolled.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEnrolled.setAdapter(adapter);

        toolbarEnrolled.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        btnBackToEvent.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        loadEnrolledEntrants();
    }

    /**
     * Loads the enrolled entrants for the current event from Firestore
     *
     * Each Firestore document is converted into an EnrolledEntrant object
     *
     * Displays the total number of enrolled entrants in the statistics TextView
     */
    private void loadEnrolledEntrants() {
        enrolledRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Failed to load enrolled entrants", Toast.LENGTH_SHORT).show();
                return;
            }

            enrolledEntrants.clear();

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

                    enrolledEntrants.add(new EnrolledEntrant(userId, name, email, status));
                }
            }

            adapter.notifyDataSetChanged();
            tvEnrolledStats.setText("Total Enrolled: " + enrolledEntrants.size());
        });
    }
}
