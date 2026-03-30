package com.example.eventsapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment that displays the final list of entrants enrolled in an event.
 *
 * Fulfills US 02.06.03 - As an organizer I want to see a final list of entrants
 * who enrolled for the event.
 */
public class EnrolledFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_OPEN_INVITE_ON_LOAD = "open_invite_on_load";

    private MaterialToolbar toolbarEnrolled;
    private RecyclerView rvEnrolled;
    private TextView tvEnrolledStats;
    private TextInputEditText etSearchEnrolled;
    private MaterialButton btnInviteEntrant;
    private MaterialButton btnExportCsv;
    private MaterialButton btnSeeCancelled;

    private final FirestoreNotificationHelper notificationHelper = new FirestoreNotificationHelper();
    private FirebaseFirestore db;
    private CollectionReference enrolledRef;
    private CollectionReference entrantsRef;

    private final ArrayList<EnrolledEntrant> allEnrolledEntrants = new ArrayList<>();
    private final ArrayList<EnrolledEntrant> filteredEnrolledEntrants = new ArrayList<>();
    private final ArrayList<String> coOrganizerIds = new ArrayList<>();
    private EnrolledEntrantAdapter adapter;

    private String eventId;
    private boolean isPrivateEvent;
    private boolean openInviteOnLoad;
    private boolean inviteDialogOpenedFromArgs;
    private String createdByUserId = "";

    public EnrolledFragment() {
        super(R.layout.view_enrolled);
    }

    public static EnrolledFragment newInstance(String eventId) {
        return newInstance(eventId, false);
    }

    public static EnrolledFragment newInstance(String eventId, boolean openInviteOnLoad) {
        EnrolledFragment fragment = new EnrolledFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putBoolean(ARG_OPEN_INVITE_ON_LOAD, openInviteOnLoad);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            openInviteOnLoad = getArguments().getBoolean(ARG_OPEN_INVITE_ON_LOAD, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbarEnrolled = view.findViewById(R.id.toolbar_enrolled);
        rvEnrolled = view.findViewById(R.id.rv_enrolled);
        tvEnrolledStats = view.findViewById(R.id.tv_enrolled_stats);
        etSearchEnrolled = view.findViewById(R.id.et_search_enrolled);
        btnInviteEntrant = view.findViewById(R.id.btn_invite_entrant);
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
        entrantsRef = db.collection("events")
                .document(eventId)
                .collection("entrants");

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

        btnInviteEntrant.setOnClickListener(v -> showEntrantPickerDialog());
        btnExportCsv.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Export CSV", Toast.LENGTH_SHORT).show());
        btnSeeCancelled.setOnClickListener(v -> openCancelledFragment());

        loadEventConfiguration();
        loadEnrolledEntrants();
    }

    private void loadEventConfiguration() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!isAdded()) {
                        return;
                    }

                    isPrivateEvent = Boolean.TRUE.equals(eventDoc.getBoolean("isPrivate"));
                    createdByUserId = valueOrEmpty(eventDoc.getString("createdBy"));

                    coOrganizerIds.clear();
                    List<String> loadedCoOrganizers = (List<String>) eventDoc.get("coOrganizerIds");
                    if (loadedCoOrganizers != null) {
                        coOrganizerIds.addAll(loadedCoOrganizers);
                    }

                    updateInviteButtonVisibility();
                    maybeOpenInviteDialog();
                })
                .addOnFailureListener(unused -> updateInviteButtonVisibility());
    }

    private void updateInviteButtonVisibility() {
        if (!isAdded()) {
            return;
        }
        btnInviteEntrant.setVisibility(isPrivateEvent ? View.VISIBLE : View.GONE);
    }

    private void maybeOpenInviteDialog() {
        if (!openInviteOnLoad || inviteDialogOpenedFromArgs || !isPrivateEvent || !isAdded()) {
            return;
        }
        inviteDialogOpenedFromArgs = true;
        showEntrantPickerDialog();
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
            inviteEntrant(selectedUser);
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
                        if (isEligibleEntrantInviteCandidate(user)) {
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

    private boolean isEligibleEntrantInviteCandidate(Users user) {
        String userId = valueOrEmpty(user.getId());
        if (userId.isEmpty() || isEventOrganizer(userId)) {
            return false;
        }

        for (EnrolledEntrant entrant : allEnrolledEntrants) {
            if (userId.equals(valueOrEmpty(entrant.getUserId()))) {
                return false;
            }
        }

        return true;
    }

    private void inviteEntrant(Users user) {
        String userId = valueOrEmpty(user.getId());
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "Selected user is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isEventOrganizer(userId)) {
            Toast.makeText(requireContext(), "Organizers cannot join the entrant pool", Toast.LENGTH_SHORT).show();
            return;
        }

        entrantsRef.whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    DocumentReference entrantRef;
                    String existingStatus = null;
                    if (querySnapshot.isEmpty()) {
                        entrantRef = entrantsRef.document(userId);
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
                    data.put("status", "INVITED");
                    data.put("statusCode", 1);

                    entrantRef.set(data, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                notificationHelper.sendInvitationNotification(userId, eventId);
                                writeInvitedHistoryRecord(userId);
                                Toast.makeText(requireContext(), "Entrant invitation sent", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(unused ->
                                    Toast.makeText(requireContext(), "Failed to invite entrant", Toast.LENGTH_SHORT).show());
                });
    }

    private void writeInvitedHistoryRecord(String userId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (eventDoc == null || !eventDoc.exists()) {
                        return;
                    }

                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("id", eventId);
                    eventData.put("name", eventDoc.getString("name"));
                    eventData.put("description", eventDoc.getString("description"));
                    eventData.put("posterUrl", eventDoc.getString("posterUrl"));
                    eventData.put("event_date", eventDoc.getString("event_date"));
                    eventData.put("registration_start", eventDoc.getString("registration_start"));
                    eventData.put("registration_end", eventDoc.getString("registration_end"));

                    Long amountLong = eventDoc.getLong("amount");
                    eventData.put("amount", amountLong != null ? amountLong : 0L);

                    Long sampleLong = eventDoc.getLong("sampleSize");
                    eventData.put("sampleSize", sampleLong != null ? sampleLong : 0L);

                    EventCleanupHelper.writeHistoryRecord(userId, eventId, eventData, "INVITED");
                });
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.CANADA).contains(query);
    }

    private boolean isEventOrganizer(String userId) {
        return createdByUserId.equals(userId) || coOrganizerIds.contains(userId);
    }

    private String buildUserDisplayName(Users user) {
        String name = valueOrEmpty(user.getName());
        return name.isEmpty() ? "Unnamed User" : name;
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

    private void openCancelledFragment() {
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, new CancelledFragment())
                .addToBackStack(null)
                .commit();
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
            String name = user.getName() == null || user.getName().trim().isEmpty()
                    ? "Unnamed User" : user.getName().trim();
            holder.tvName.setText(name);

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
