package com.example.eventsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A Fragment representing the organizer's view of an event's waitlist.
 * Allows the organizer to view entrants in the pool, and to remove or replace
 * existing entrants. Integrates with Firebase Firestore for real-time updates.
 */
public class OrganizerWaitlistFragment extends Fragment {

    private FirebaseFirestore db;
    private String eventId;
    private RecyclerView rvWaitlist;
    private OrganizerEntrantAdapter adapter;
    private List<Entrant> entrantList = new ArrayList<>();

    /**
     * Default constructor for the OrganizerWaitlistFragment.
     */
    public OrganizerWaitlistFragment() {
        super(R.layout.fragment_organizer_waitlist);
    }

    /**
     * Factory method to create a new instance of this fragment using the provided event ID.
     *
     * @param eventId The ID of the event whose waitlist is being viewed.
     * @return A new instance of OrganizerWaitlistFragment.
     */
    public static OrganizerWaitlistFragment newInstance(String eventId) {
        OrganizerWaitlistFragment fragment = new OrganizerWaitlistFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Initializes the fragment, setting up the Firestore instance and retrieving arguments.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
        }
    }

    /**
     * Called immediately after {@link #onCreateView} has returned, but before any saved state has been restored in to the view.
     * Initializes the UI components and standard adapters.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_organizer_waitlist);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        rvWaitlist = view.findViewById(R.id.rv_organizer_waitlist);
        adapter = new OrganizerEntrantAdapter(entrantList, new OrganizerEntrantAdapter.OnEntrantActionListener() {
            @Override
            public void onRemove(Entrant entrant) {
                removeEntrant(entrant);
            }

            @Override
            public void onReplace(Entrant entrant) {
                replaceEntrant(entrant);
            }
        });
        rvWaitlist.setAdapter(adapter);

        loadWaitlistData();
    }

    /**
     * Fetches entrants currently active in this event and listens for real-time updates.
     * Status codes tracked: 0 (In Pool), 1 (Invited), 2 (Accepted).
     * Excludes status code 3 (Cancelled), which are tracked in a separate list.
     * Implements US 02.02.01.
     */
    private void loadWaitlistData() {
        if (eventId == null || eventId.isEmpty()) return;

        db.collection("events").document(eventId)
                .collection("entrants")
                .whereIn("statusCode", Arrays.asList(0, 1, 2))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(requireContext(), "Error loading waitlist", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    entrantList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Entrant entrant = doc.toObject(Entrant.class);
                            if (entrant != null) {
                                entrant.setId(doc.getId());
                                entrantList.add(entrant);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Removes an entrant from the active waitlist by moving them to the cancelled list.
     * Updates the entrant's statusCode to 3 (Cancelled) in Firestore.
     * Implements US 02.05.03 - Backend.
     *
     * @param entrantToRemove The entrant to be removed.
     */
    private void removeEntrant(Entrant entrantToRemove) {
        if (eventId == null || eventId.isEmpty() || entrantToRemove.getId() == null) return;

        db.collection("events").document(eventId)
                .collection("entrants").document(entrantToRemove.getId())
                .update("status", "CANCELLED", "statusCode", 3)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Entrant removed.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Replaces an entrant by moving the current entrant to the cancelled list
     * and randomly drawing a new one from the uninvited pool.
     * Implements US 02.05.03 - Backend.
     * TODO: In the future, add a confirmation dialog here before executing.
     *
     * @param entrantToReplace The entrant to be replaced.
     */
    private void replaceEntrant(Entrant entrantToReplace) {
        if (eventId == null || eventId.isEmpty() || entrantToReplace.getId() == null) return;

        // Step 1: Move current entrant to cancelled
        db.collection("events").document(eventId)
                .collection("entrants").document(entrantToReplace.getId())
                .update("status", "CANCELLED", "statusCode", 3)
                .addOnSuccessListener(aVoid -> {
                    // Step 2: Draw Replacement
                    drawReplacementApplicant();
                });
    }

    /**
     * Randomly selects a new entrant from the pool of uninvited users (statusCode = 0)
     * and updates their status to Invited (statusCode = 1).
     * Called as part of the entrant replacement workflow.
     */
    private void drawReplacementApplicant() {
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("statusCode", 0) // Only pick from uninvited users
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No more applicants left in the pool.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<DocumentSnapshot> applicants = queryDocumentSnapshots.getDocuments();
                    DocumentSnapshot selectedApplicant = applicants.get(new Random().nextInt(applicants.size()));

                    // Update selected applicant to Invited (1)
                    db.collection("events").document(eventId)
                            .collection("entrants").document(selectedApplicant.getId())
                            .update("status", "INVITED", "statusCode", 1)
                            .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Replacement drawn successfully!", Toast.LENGTH_SHORT).show());
                });
    }
}