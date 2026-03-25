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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * US 02.06.01: View list of all chosen entrants who are invited to apply.
 */
public class ViewEntrantsFragment extends Fragment {

    private final FirestoreNotificationHelper notificationHelper = new FirestoreNotificationHelper();
    private String eventId;
    private RecyclerView rvWaitlist;
    private TextView tvStats;
    private EntrantAdapter adapter;
    private final List<Entrant> allEntrants = new ArrayList<>();
    private final List<Entrant> filteredEntrants = new ArrayList<>();
    private ListenerRegistration listenerRegistration;
    private TextInputEditText etSearch;
    private FirebaseFirestore db;
    private MaterialToolbar toolbar;
    private MaterialButton btnAddApplicant;
    private MaterialButton btnRunSampling;
    private MaterialButton btnNotifyChosen;
    private MaterialButton btnExportCsv;
    private MaterialButton btnViewEnrolled;
    private MaterialButton btnSeeCancelled;
    private MaterialButton btnInviteCoOrganizer;
    private boolean isPrivateEvent;
    private String createdByUserId = "";
    private final List<String> coOrganizerIds = new ArrayList<>();
    private final List<String> pendingCoOrganizerIds = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId == null) eventId = "";
        } else {
            eventId = "";
        }
        db = FirebaseFirestore.getInstance();
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

        toolbar = view.findViewById(R.id.toolbar_waitlist);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        rvWaitlist = view.findViewById(R.id.rv_waitlist);
        tvStats = view.findViewById(R.id.tv_waitlist_stats);
        etSearch = view.findViewById(R.id.et_search_waitlist);

        btnAddApplicant = view.findViewById(R.id.btn_add_applicant);
        btnRunSampling = view.findViewById(R.id.btn_run_sampling);
        btnNotifyChosen = view.findViewById(R.id.btn_notify_chosen);
        btnExportCsv = view.findViewById(R.id.btn_export_csv);
        btnViewEnrolled = view.findViewById(R.id.btn_view_enrolled);
        btnSeeCancelled = view.findViewById(R.id.btn_see_cancelled);
        btnInviteCoOrganizer = view.findViewById(R.id.btn_invite_coorganizer);

        adapter = new EntrantAdapter(filteredEntrants);
        rvWaitlist.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWaitlist.setAdapter(adapter);

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

        btnAddApplicant.setOnClickListener(v -> {
            if (isPrivateEvent) {
                showUserSearchDialog(false);
            } else {
                showAddApplicantDialog();
            }
        });
        btnRunSampling.setOnClickListener(v -> runSampling());
        btnViewEnrolled.setOnClickListener(v -> openEnrolledFragment());
        btnNotifyChosen.setOnClickListener(v -> notifyChosenEntrants());
        btnInviteCoOrganizer.setOnClickListener(v -> showUserSearchDialog(true));
        btnExportCsv.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Export CSV", Toast.LENGTH_SHORT).show());
        btnSeeCancelled.setOnClickListener(v ->
                Toast.makeText(requireContext(), "See Cancelled Entrants", Toast.LENGTH_SHORT).show());

        loadEventConfiguration();
    }

    private void loadEventConfiguration() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    isPrivateEvent = Boolean.TRUE.equals(eventDoc.getBoolean("isPrivate"));
                    createdByUserId = valueOrEmpty(eventDoc.getString("createdBy"));

                    coOrganizerIds.clear();
                    List<String> loadedCoOrganizers = (List<String>) eventDoc.get("coOrganizerIds");
                    if (loadedCoOrganizers != null) {
                        coOrganizerIds.addAll(loadedCoOrganizers);
                    }

                    pendingCoOrganizerIds.clear();
                    List<String> loadedPendingCoOrganizers = (List<String>) eventDoc.get("pendingCoOrganizerIds");
                    if (loadedPendingCoOrganizers != null) {
                        pendingCoOrganizerIds.addAll(loadedPendingCoOrganizers);
                    }

                    updateActionLabels();
                    loadChosenEntrants();
                })
                .addOnFailureListener(unused -> {
                    updateActionLabels();
                    loadChosenEntrants();
                });
    }

    private void updateActionLabels() {
        if (!isAdded()) {
            return;
        }

        toolbar.setTitle(isPrivateEvent ? "Private Event Access" : "Chosen Entrants");
        btnAddApplicant.setText(isPrivateEvent ? "Invite Entrant" : "Add Applicant");
        btnInviteCoOrganizer.setVisibility(View.VISIBLE);
    }

    private void openEnrolledFragment() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, EnrolledFragment.newInstance(eventId))
                .addToBackStack(null)
                .commit();
    }

    private void loadChosenEntrants() {
        CollectionReference entrantsRef = db.collection("events").document(eventId).collection("entrants");

        Query query = isPrivateEvent
                ? entrantsRef.whereIn("status", Arrays.asList("PRIVATE_INVITED", "APPLIED", "INVITED", "ACCEPTED"))
                : entrantsRef.whereIn("status", Arrays.asList("INVITED", "ACCEPTED"));

        listenerRegistration = query.addSnapshotListener((value, error) -> {
            if (error != null || value == null || !isAdded()) {
                return;
            }

            allEntrants.clear();
            for (QueryDocumentSnapshot doc : value) {
                String id = doc.getString("id");
                String name = doc.getString("name");
                String email = doc.getString("email");
                String statusStr = doc.getString("status");
                Entrant.Status status = parseStatus(statusStr);
                Entrant entrant = new Entrant(id, eventId, name, email, status);
                entrant.setUserId(doc.getString("userId"));
                entrant.setStatusCode(doc.getLong("statusCode") != null ? doc.getLong("statusCode").intValue() : 0);
                allEntrants.add(entrant);
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
        String q = query != null ? query.toLowerCase(Locale.CANADA).trim() : "";
        for (Entrant e : allEntrants) {
            if (q.isEmpty()
                    || containsIgnoreCase(e.getName(), q)
                    || containsIgnoreCase(e.getEmail(), q)) {
                filteredEntrants.add(e);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateStats() {
        int privateInvites = 0;
        int waitlisted = 0;
        int invited = 0;
        int accepted = 0;

        for (Entrant entrant : allEntrants) {
            if (entrant.getStatus() == Entrant.Status.PRIVATE_INVITED) {
                privateInvites++;
            } else if (entrant.getStatus() == Entrant.Status.APPLIED) {
                waitlisted++;
            } else if (entrant.getStatus() == Entrant.Status.INVITED) {
                invited++;
            } else if (entrant.getStatus() == Entrant.Status.ACCEPTED) {
                accepted++;
            }
        }

        if (isPrivateEvent) {
            tvStats.setText(String.format(Locale.CANADA,
                    "Pending Waitlist Invites: %d\nOn Waitlist: %d\nChosen Entrants: %d\nAccepted: %d\nCo-Organizers: %d",
                    privateInvites, waitlisted, invited, accepted, coOrganizerIds.size()));
        } else {
            tvStats.setText(String.format(Locale.CANADA,
                    "Total Invited: %d\nTotal Accepted: %d\nChosen Entrants: %d",
                    invited, accepted, allEntrants.size()));
        }
    }

    private void showAddApplicantDialog() {
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

        db.collection("events").document(eventId).collection("entrants").document(id)
                .set(data)
                .addOnSuccessListener(v -> Toast.makeText(requireContext(), "Applicant added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to add", Toast.LENGTH_SHORT).show());
    }

    private void showUserSearchDialog(boolean coOrganizerInvite) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText editName = new EditText(requireContext());
        editName.setHint("Search by name");
        layout.addView(editName);

        EditText editEmail = new EditText(requireContext());
        editEmail.setHint("Search by email");
        editEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(editEmail);

        EditText editPhone = new EditText(requireContext());
        editPhone.setHint("Search by phone");
        editPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(editPhone);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(coOrganizerInvite ? "Invite Co-Organizer" : "Invite Entrant")
                .setView(layout)
                .setPositiveButton("Search", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nameFilter = valueOrEmpty(editName.getText() != null ? editName.getText().toString() : "");
            String emailFilter = valueOrEmpty(editEmail.getText() != null ? editEmail.getText().toString() : "");
            String phoneFilter = valueOrEmpty(editPhone.getText() != null ? editPhone.getText().toString() : "");

            if (nameFilter.isEmpty() && emailFilter.isEmpty() && phoneFilter.isEmpty()) {
                Toast.makeText(requireContext(), "Enter at least one search field", Toast.LENGTH_SHORT).show();
                return;
            }

            searchUsers(nameFilter, emailFilter, phoneFilter, coOrganizerInvite, dialog);
        }));

        dialog.show();
    }

    private void searchUsers(String nameFilter, String emailFilter, String phoneFilter,
                             boolean coOrganizerInvite, AlertDialog sourceDialog) {
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Users> matches = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Users user = doc.toObject(Users.class);
                        if (user == null) {
                            continue;
                        }
                        user.setId(doc.getId());
                        if (matchesSearch(user, nameFilter, emailFilter, phoneFilter)) {
                            matches.add(user);
                        }
                    }

                    if (matches.isEmpty()) {
                        Toast.makeText(requireContext(), "No matching users found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    sourceDialog.dismiss();
                    showUserSelectionDialog(matches, coOrganizerInvite);
                })
                .addOnFailureListener(unused ->
                        Toast.makeText(requireContext(), "Failed to search users", Toast.LENGTH_SHORT).show());
    }

    private boolean matchesSearch(Users user, String nameFilter, String emailFilter, String phoneFilter) {
        return containsIgnoreCase(user.getName(), nameFilter)
                && containsIgnoreCase(user.getEmail(), emailFilter)
                && normalizePhone(user.getPhoneNumber()).contains(normalizePhone(phoneFilter));
    }

    private void showUserSelectionDialog(List<Users> matches, boolean coOrganizerInvite) {
        String[] labels = new String[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            Users user = matches.get(i);
            labels[i] = valueOrEmpty(user.getName()) + " - "
                    + valueOrEmpty(user.getEmail()) + " - "
                    + valueOrEmpty(user.getPhoneNumber());
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(coOrganizerInvite ? "Select Co-Organizer" : "Select Entrant")
                .setItems(labels, (dialog, which) -> {
                    Users selectedUser = matches.get(which);
                    if (coOrganizerInvite) {
                        inviteCoOrganizer(selectedUser);
                    } else {
                        invitePrivateEntrant(selectedUser);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void invitePrivateEntrant(Users user) {
        String userId = user.getId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Selected user is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isEventOrganizer(userId)) {
            Toast.makeText(requireContext(), "Organizers cannot join the entrant pool", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    DocumentReference entrantRef;
                    String existingStatus = null;
                    if (querySnapshot.isEmpty()) {
                        entrantRef = db.collection("events").document(eventId).collection("entrants").document(userId);
                    } else {
                        entrantRef = querySnapshot.getDocuments().get(0).getReference();
                        existingStatus = querySnapshot.getDocuments().get(0).getString("status");
                    }

                    if ("PRIVATE_INVITED".equals(existingStatus)
                            || "APPLIED".equals(existingStatus)
                            || "INVITED".equals(existingStatus)
                            || "ACCEPTED".equals(existingStatus)) {
                        Toast.makeText(requireContext(), "User is already linked to this event", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", entrantRef.getId());
                    data.put("eventId", eventId);
                    data.put("name", valueOrEmpty(user.getName()));
                    data.put("email", valueOrEmpty(user.getEmail()));
                    data.put("phoneNumber", valueOrEmpty(user.getPhoneNumber()));
                    data.put("userId", userId);
                    data.put("status", "PRIVATE_INVITED");
                    data.put("statusCode", 4);

                    entrantRef.set(data, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                notificationHelper.sendPrivateWaitlistInvitationNotification(userId, eventId);
                                Toast.makeText(requireContext(), "Private waitlist invitation sent", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(unused ->
                                    Toast.makeText(requireContext(), "Failed to invite entrant", Toast.LENGTH_SHORT).show());
                });
    }

    private void inviteCoOrganizer(Users user) {
        String userId = user.getId();
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Selected user is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        if (createdByUserId.equals(userId) || coOrganizerIds.contains(userId)) {
            Toast.makeText(requireContext(), "User is already an organizer for this event", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pendingCoOrganizerIds.contains(userId)) {
            Toast.makeText(requireContext(), "User already has a pending co-organizer invite", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventId)
                .update("pendingCoOrganizerIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> {
                    pendingCoOrganizerIds.add(userId);
                    notificationHelper.sendCoOrganizerInvitationNotification(userId, eventId);
                    Toast.makeText(requireContext(), "Co-organizer invite sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(unused ->
                        Toast.makeText(requireContext(), "Failed to invite co-organizer", Toast.LENGTH_SHORT).show());
    }

    private boolean isEventOrganizer(String userId) {
        return createdByUserId.equals(userId) || coOrganizerIds.contains(userId);
    }

    private void runSampling() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    Long sampleSizeLong = eventDoc != null ? eventDoc.getLong("sampleSize") : null;
                    int sampleSize = sampleSizeLong != null ? sampleSizeLong.intValue() : 0;
                    if (sampleSize <= 0) {
                        Toast.makeText(requireContext(), "Set sample size in event first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CollectionReference entrantsRef = db.collection("events").document(eventId).collection("entrants");
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
                                WriteBatch batch = db.batch();
                                for (int i = 0; i < toInvite; i++) {
                                    DocumentReference ref = applicants.get(i).getReference();
                                    batch.update(ref, "status", "INVITED", "statusCode", 1);
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

    /**
     * Sends notifications to all entrants who were selected in the lottery.
     */
    private void notifyChosenEntrants() {
        if (allEntrants.isEmpty()) {
            Toast.makeText(requireContext(), "No entrants available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Entrant> chosenEntrants = new ArrayList<>();
        for (Entrant entrant : allEntrants) {
            if (entrant != null && entrant.getStatus() == Entrant.Status.INVITED) {
                chosenEntrants.add(entrant);
            }
        }

        if (chosenEntrants.isEmpty()) {
            Toast.makeText(requireContext(), "No chosen entrants to notify", Toast.LENGTH_SHORT).show();
            return;
        }

        int sentCount = 0;
        int skippedCount = 0;

        for (Entrant entrant : chosenEntrants) {
            String userId = entrant.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                skippedCount++;
                continue;
            }

            notificationHelper.sendInvitationNotification(userId, eventId);
            sentCount++;
        }

        String message;
        if (sentCount == 0) {
            message = "Chosen entrants found, but no linked users could be notified";
        } else if (skippedCount == 0) {
            message = "Notifications sent to " + sentCount + " entrants";
        } else {
            message = "Notifications sent to " + sentCount + " entrants, skipped " + skippedCount;
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.CANADA).contains(query.toLowerCase(Locale.CANADA));
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
