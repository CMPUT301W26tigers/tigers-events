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
    private EntrantAdapter adapter;
    private final List<Entrant> allEntrants = new ArrayList<>();
    private final List<Entrant> filteredEntrants = new ArrayList<>();
    private ListenerRegistration listenerRegistration;
    private TextInputEditText etSearch;
    private FirebaseFirestore db;
    private MaterialToolbar toolbar;
    private MaterialButton btnAddApplicant;
    private MaterialButton btnRunLottery;
    private MaterialButton btnExportCsv;
    private MaterialButton btnSeeCancelled;
    private MaterialButton btnInviteCoOrganizer;
    private boolean isPrivateEvent;
    private boolean openInviteOnLoad;
    private boolean inviteDialogOpenedFromArgs;
    private String createdByUserId = "";
    private final List<String> coOrganizerIds = new ArrayList<>();
    private final List<String> pendingCoOrganizerIds = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId == null) eventId = "";
            openInviteOnLoad = getArguments().getBoolean("openInviteOnLoad", false);
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
        etSearch = view.findViewById(R.id.et_search_waitlist);

        btnAddApplicant = view.findViewById(R.id.btn_add_applicant);
        btnRunLottery = view.findViewById(R.id.btn_run_lottery);
        btnExportCsv = view.findViewById(R.id.btn_export_csv);
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
                showEntrantPickerDialog();
            } else {
                showAddApplicantDialog();
            }
        });
        btnRunLottery.setOnClickListener(v -> runLottery());
        btnInviteCoOrganizer.setOnClickListener(v -> showUserSearchDialog(true));
        btnExportCsv.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Export CSV", Toast.LENGTH_SHORT).show());
        btnSeeCancelled.setOnClickListener(v -> openCancelledFragment());

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
                    maybeOpenPrivateInviteDialog();
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

        toolbar.setTitle("Waitlist");
        btnAddApplicant.setText(isPrivateEvent ? "Invite Entrant" : "Add Applicant");
        btnInviteCoOrganizer.setVisibility(View.VISIBLE);
    }

    private void maybeOpenPrivateInviteDialog() {
        if (!openInviteOnLoad || inviteDialogOpenedFromArgs || !isPrivateEvent || !isAdded()) {
            return;
        }
        inviteDialogOpenedFromArgs = true;
        showEntrantPickerDialog();
    }

    private void openCancelledFragment() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, new CancelledFragment())
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

    private void showEntrantPickerDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_picker, null);
        TextInputEditText etUserSearch = dialogView.findViewById(R.id.et_user_picker_search);
        RecyclerView rvUserResults = dialogView.findViewById(R.id.rv_user_picker_results);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_user_picker_empty);

        List<Users> eligibleUsers = new ArrayList<>();
        List<Users> filteredUsers = new ArrayList<>();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Invite Entrant")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        UserPickerAdapter pickerAdapter = new UserPickerAdapter(filteredUsers, selectedUser -> {
            dialog.dismiss();
            invitePrivateEntrant(selectedUser);
        });

        rvUserResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUserResults.setAdapter(pickerAdapter);

        tvEmpty.setText("Loading users...");
        tvEmpty.setVisibility(View.VISIBLE);
        rvUserResults.setVisibility(View.GONE);

        etUserSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEligibleUsers(eligibleUsers, filteredUsers, pickerAdapter, rvUserResults, tvEmpty,
                        s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eligibleUsers.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Users user = doc.toObject(Users.class);
                        if (user == null) {
                            continue;
                        }
                        user.setId(doc.getId());
                        if (isEligiblePrivateInviteCandidate(user)) {
                            eligibleUsers.add(user);
                        }
                    }

                    eligibleUsers.sort((first, second) ->
                            buildUserDisplayName(first).compareToIgnoreCase(buildUserDisplayName(second)));

                    String query = etUserSearch.getText() != null ? etUserSearch.getText().toString() : "";
                    filterEligibleUsers(eligibleUsers, filteredUsers, pickerAdapter, rvUserResults, tvEmpty, query);
                })
                .addOnFailureListener(unused -> {
                    tvEmpty.setText("Failed to load users");
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvUserResults.setVisibility(View.GONE);
                });

        dialog.show();
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

    private void filterEligibleUsers(List<Users> eligibleUsers,
                                     List<Users> filteredUsers,
                                     UserPickerAdapter pickerAdapter,
                                     RecyclerView rvUserResults,
                                     TextView tvEmpty,
                                     String query) {
        filteredUsers.clear();
        String normalizedQuery = valueOrEmpty(query).toLowerCase(Locale.CANADA);
        String normalizedPhoneQuery = normalizePhone(query);

        for (Users user : eligibleUsers) {
            String userName = buildUserDisplayName(user).toLowerCase(Locale.CANADA);
            String userEmail = valueOrEmpty(user.getEmail()).toLowerCase(Locale.CANADA);
            String userPhone = normalizePhone(user.getPhoneNumber());

            if (normalizedQuery.isEmpty()
                    || userName.contains(normalizedQuery)
                    || userEmail.contains(normalizedQuery)
                    || (!normalizedPhoneQuery.isEmpty() && userPhone.contains(normalizedPhoneQuery))) {
                filteredUsers.add(user);
            }
        }

        pickerAdapter.notifyDataSetChanged();

        if (filteredUsers.isEmpty()) {
            tvEmpty.setText(eligibleUsers.isEmpty() ? "No eligible users found" : "No matching users");
            tvEmpty.setVisibility(View.VISIBLE);
            rvUserResults.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        rvUserResults.setVisibility(View.VISIBLE);
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

    private boolean isEligiblePrivateInviteCandidate(Users user) {
        String userId = valueOrEmpty(user.getId());
        if (userId.isEmpty() || isEventOrganizer(userId)) {
            return false;
        }

        for (Entrant entrant : allEntrants) {
            if (userId.equals(valueOrEmpty(entrant.getUserId()))) {
                return false;
            }
        }

        return true;
    }

    private String buildUserDisplayName(Users user) {
        String name = valueOrEmpty(user.getName());
        return name.isEmpty() ? "Unnamed User" : name;
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

    private void runLottery() {
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
                                List<String> invitedUserIds = new ArrayList<>();
                                for (int i = 0; i < toInvite; i++) {
                                    DocumentReference ref = applicants.get(i).getReference();
                                    batch.update(ref, "status", "INVITED", "statusCode", 1);
                                    String invitedUserId = applicants.get(i).getString("userId");
                                    if (invitedUserId != null && !invitedUserId.trim().isEmpty()) {
                                        invitedUserIds.add(invitedUserId);
                                    }
                                }
                                batch.commit()
                                        .addOnSuccessListener(v -> {
                                            for (String invitedUserId : invitedUserIds) {
                                                notificationHelper.sendInvitationNotification(invitedUserId, eventId);
                                                EventCleanupHelper.updateHistoryStatus(invitedUserId, eventId, "INVITED");
                                            }
                                            Toast.makeText(requireContext(),
                                                    "Lottery run for " + toInvite + " applicants", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                                "Lottery failed", Toast.LENGTH_SHORT).show());
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

    private static class UserPickerAdapter extends RecyclerView.Adapter<UserPickerAdapter.UserPickerViewHolder> {
        interface OnUserSelectedListener {
            void onUserSelected(Users user);
        }

        private final List<Users> users;
        private final OnUserSelectedListener onUserSelectedListener;
        UserPickerAdapter(List<Users> users, OnUserSelectedListener onUserSelectedListener) {
            this.users = users;
            this.onUserSelectedListener = onUserSelectedListener;
        }

        @NonNull
        @Override
        public UserPickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_picker, parent, false);
            return new UserPickerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserPickerViewHolder holder, int position) {
            Users user = users.get(position);
            holder.tvName.setText(user.getName() == null || user.getName().trim().isEmpty()
                    ? "Unnamed User" : user.getName().trim());

            String email = user.getEmail() == null ? "" : user.getEmail().trim();
            String phone = user.getPhoneNumber() == null ? "" : user.getPhoneNumber().trim();
            if (!email.isEmpty() && !phone.isEmpty()) {
                holder.tvDetails.setText(email + " | " + phone);
            } else if (!email.isEmpty()) {
                holder.tvDetails.setText(email);
            } else if (!phone.isEmpty()) {
                holder.tvDetails.setText(phone);
            } else {
                holder.tvDetails.setText("No contact info");
            }

            holder.itemView.setOnClickListener(v -> onUserSelectedListener.onUserSelected(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserPickerViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvDetails;

            UserPickerViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_user_picker_name);
                tvDetails = itemView.findViewById(R.id.tv_user_picker_details);
            }
        }
    }
}
