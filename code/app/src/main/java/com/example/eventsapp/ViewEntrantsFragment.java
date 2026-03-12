package com.example.eventsapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US 02.06.01: View list of all chosen entrants who are invited to apply.
 */
public class ViewEntrantsFragment extends Fragment {

    private String eventId;
    private RecyclerView rvWaitlist;
    private TextView tvStats;
    private EntrantAdapter adapter;
    private List<Entrant> allEntrants = new ArrayList<>();
    private List<Entrant> filteredEntrants = new ArrayList<>();
    private ListenerRegistration listenerRegistration;
    private TextInputEditText etSearch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId == null) eventId = "";
        } else {
            eventId = "";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_waitlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_waitlist);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        rvWaitlist = view.findViewById(R.id.rv_waitlist);
        tvStats = view.findViewById(R.id.tv_waitlist_stats);

        adapter = new EntrantAdapter(filteredEntrants);
        rvWaitlist.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWaitlist.setAdapter(adapter);

        etSearch = view.findViewById(R.id.et_search_waitlist);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntrants(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadChosenEntrants();

        view.findViewById(R.id.btn_add_applicant).setOnClickListener(v -> showAddApplicantDialog());
        view.findViewById(R.id.btn_run_sampling).setOnClickListener(v -> runSampling());

        view.findViewById(R.id.btn_export_csv).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Export CSV", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_see_cancelled).setOnClickListener(v ->
                Toast.makeText(requireContext(), "See Cancelled Entrants", Toast.LENGTH_SHORT).show());
    }

    private void loadChosenEntrants() {
        CollectionReference entrantsRef = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).collection("entrants");

        Query query = entrantsRef.whereIn("status", 
                java.util.Arrays.asList("INVITED", "ACCEPTED"));

        listenerRegistration = query.addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value == null) return;
            if (!isAdded()) return;

            allEntrants.clear();
            for (QueryDocumentSnapshot doc : value) {
                String id = doc.getString("id");
                String name = doc.getString("name");
                String email = doc.getString("email");
                String statusStr = doc.getString("status");
                Entrant.Status status = parseStatus(statusStr);
                allEntrants.add(new Entrant(id, eventId, name, email, status));
            }
            String queryStr = (etSearch != null && etSearch.getText() != null) ? etSearch.getText().toString() : "";
            filterEntrants(queryStr);
            updateStats();
        });
    }

    private Entrant.Status parseStatus(String s) {
        if (s == null) return Entrant.Status.INVITED;
        try {
            return Entrant.Status.valueOf(s);
        } catch (Exception e) {
            return Entrant.Status.INVITED;
        }
    }

    private void filterEntrants(String query) {
        filteredEntrants.clear();
        String q = query != null ? query.toLowerCase().trim() : "";
        for (Entrant e : allEntrants) {
            if (q.isEmpty() || (e.getName() != null && e.getName().toLowerCase().contains(q))
                    || (e.getEmail() != null && e.getEmail().toLowerCase().contains(q))) {
                filteredEntrants.add(e);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateStats() {
        int invited = 0, accepted = 0;
        for (Entrant e : allEntrants) {
            if (e.getStatus() == Entrant.Status.INVITED) invited++;
            else if (e.getStatus() == Entrant.Status.ACCEPTED) accepted++;
        }
        tvStats.setText(String.format("Total Invited: %d\nTotal Accepted: %d\nChosen Entrants: %d",
                invited, accepted, allEntrants.size()));
    }

    private void showAddApplicantDialog() {
        View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        EditText editName = new EditText(requireContext());
        editName.setHint("Name");
        EditText editEmail = new EditText(requireContext());
        editEmail.setHint("Email");
        editEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(editName);
        layout.addView(editEmail);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Applicant")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String name = editName.getText() != null ? editName.getText().toString().trim() : "";
                    String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addApplicant(name, email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addApplicant(String name, String email) {
        String id = java.util.UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("eventId", eventId);
        data.put("name", name);
        data.put("email", email);
        data.put("status", "APPLIED");

        FirebaseFirestore.getInstance()
                .collection("events").document(eventId).collection("entrants").document(id)
                .set(data)
                .addOnSuccessListener(v -> Toast.makeText(requireContext(), "Applicant added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to add", Toast.LENGTH_SHORT).show());
    }

    private void runSampling() {
        FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    DocumentSnapshot doc = eventDoc;
                    Long sampleSizeLong = doc != null ? doc.getLong("sampleSize") : null;
                    int sampleSize = sampleSizeLong != null ? sampleSizeLong.intValue() : 0;
                    if (sampleSize <= 0) {
                        Toast.makeText(requireContext(), "Set sample size in event first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CollectionReference entrantsRef = FirebaseFirestore.getInstance()
                            .collection("events").document(eventId).collection("entrants");
                    entrantsRef.whereEqualTo("status", "APPLIED")
                            .get()
                            .addOnSuccessListener(appliedQuery -> {
                                List<QueryDocumentSnapshot> applicants = new ArrayList<>();
                                for (QueryDocumentSnapshot d : appliedQuery) applicants.add(d);
                                if (applicants.isEmpty()) {
                                    Toast.makeText(requireContext(), "No applicants to sample", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Collections.shuffle(applicants);
                                int toInvite = Math.min(sampleSize, applicants.size());
                                WriteBatch batch = FirebaseFirestore.getInstance().batch();
                                for (int i = 0; i < toInvite; i++) {
                                    DocumentReference ref = applicants.get(i).getReference();
                                    batch.update(ref, "status", "INVITED");
                                }
                                batch.commit()
                                        .addOnSuccessListener(v -> Toast.makeText(requireContext(),
                                                "Invited " + toInvite + " applicants", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                                "Sampling failed", Toast.LENGTH_SHORT).show());
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
