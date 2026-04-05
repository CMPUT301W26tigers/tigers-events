package com.example.eventsapp;

import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Package-private utility class that drives the co-organizer invitation workflow.
 *
 * <p>Entry points are {@link #showInviteDialog} (shows a searchable user-picker dialog)
 * and {@link #inviteCoOrganizer} (sends the actual invitation without showing a dialog).
 * All methods are stateless and static; instances cannot be created.
 */
final class CoOrganizerInviteHelper {

    private CoOrganizerInviteHelper() {}

    /**
     * Fetches the event document and, if it exists, opens a searchable user-picker dialog
     * that lets the organizer choose a co-organizer to invite.
     *
     * <p>Users who are already the creator, a confirmed co-organizer, or have a pending
     * invitation are excluded from the picker list.
     *
     * @param fragment the host fragment used for context and lifecycle checks
     * @param db       the Firestore instance to query
     * @param eventId  the Firestore document ID of the event
     */
    static void showInviteDialog(@NonNull Fragment fragment,
                                 @NonNull FirebaseFirestore db,
                                 @NonNull String eventId) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!fragment.isAdded() || !eventDoc.exists()) {
                        return;
                    }
                    showUserPicker(fragment, db, eventId, eventDoc);
                })
                .addOnFailureListener(unused -> {
                    if (fragment.isAdded()) {
                        TigerToast.show(fragment.requireContext(), "Failed to load event", Toast.LENGTH_SHORT);
                    }
                });
    }

    private static void showUserPicker(@NonNull Fragment fragment,
                                       @NonNull FirebaseFirestore db,
                                       @NonNull String eventId,
                                       @NonNull DocumentSnapshot eventDoc) {
        View dialogView = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_user_picker, null);
        TextInputEditText etUserSearch = dialogView.findViewById(R.id.et_user_picker_search);
        RecyclerView rvUserResults = dialogView.findViewById(R.id.rv_user_picker_results);
        TextView tvEmpty = dialogView.findViewById(R.id.tv_user_picker_empty);

        List<Users> eligibleUsers = new ArrayList<>();
        List<Users> filteredUsers = new ArrayList<>();

        String createdByUserId = valueOrEmpty(eventDoc.getString("createdBy"));
        List<String> coOrganizerIds = FirestoreDataUtils.getStringList(eventDoc, "coOrganizerIds");
        List<String> pendingCoOrganizerIds = FirestoreDataUtils.getStringList(eventDoc, "pendingCoOrganizerIds");

        AlertDialog dialog = new AlertDialog.Builder(fragment.requireContext())
                .setTitle("Invite Co-Organizer")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        UserPickerAdapter pickerAdapter = new UserPickerAdapter(filteredUsers, selectedUser -> {
            dialog.dismiss();
            inviteCoOrganizer(fragment, db, eventId, selectedUser);
        });

        rvUserResults.setLayoutManager(new LinearLayoutManager(fragment.requireContext()));
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

                        String userId = valueOrEmpty(user.getId());
                        if (userId.isEmpty()
                                || userId.equals(createdByUserId)
                                || (coOrganizerIds != null && coOrganizerIds.contains(userId))
                                || (pendingCoOrganizerIds != null && pendingCoOrganizerIds.contains(userId))) {
                            continue;
                        }
                        eligibleUsers.add(user);
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

    private static void filterEligibleUsers(List<Users> eligibleUsers,
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

    /**
     * Sends a co-organizer invitation to the given user for the specified event.
     *
     * <p>The user's ID is appended to the event's {@code pendingCoOrganizerIds} array and
     * the user is removed from the entrant sub-collection so they no longer appear as a
     * regular applicant. A Firestore-backed in-app notification is also dispatched.
     *
     * @param fragment the host fragment used for context and lifecycle checks
     * @param db       the Firestore instance to update
     * @param eventId  the Firestore document ID of the event
     * @param user     the user to invite; must have a non-blank {@code id}
     */
    static void inviteCoOrganizer(@NonNull Fragment fragment,
                                  @NonNull FirebaseFirestore db,
                                  @NonNull String eventId,
                                  @NonNull Users user) {
        String userId = user.getId();
        if (userId == null || userId.trim().isEmpty()) {
            TigerToast.show(fragment.requireContext(), "Selected user is invalid", Toast.LENGTH_SHORT);
            return;
        }

        FirestoreNotificationHelper notificationHelper = new FirestoreNotificationHelper();
        db.collection("events").document(eventId)
                .update("pendingCoOrganizerIds", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(unused -> {
                    if (!fragment.isAdded()) {
                        return;
                    }
                    // Remove this user from the event's entrant pool so they don't appear as an applicant.
                    db.collection("events").document(eventId)
                            .collection("entrants").document(userId)
                            .delete();
                    notificationHelper.sendCoOrganizerInvitationNotification(userId, eventId);
                    TigerToast.show(fragment.requireContext(), "Co-organizer invite sent", Toast.LENGTH_SHORT);
                })
                .addOnFailureListener(unused -> {
                    if (fragment.isAdded()) {
                        TigerToast.show(fragment.requireContext(), "Failed to invite co-organizer", Toast.LENGTH_SHORT);
                    }
                });
    }

    private static String buildUserDisplayName(Users user) {
        String name = valueOrEmpty(user.getName());
        return name.isEmpty() ? "Unnamed User" : name;
    }

    private static String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * RecyclerView adapter that displays a filterable list of eligible users inside the
     * invite-co-organizer dialog. Selecting a row invokes {@link OnUserSelectedListener}.
     */
    private static class UserPickerAdapter extends RecyclerView.Adapter<UserPickerAdapter.UserPickerViewHolder> {

        /**
         * Callback delivered when the user taps a row in the picker list.
         */
        interface OnUserSelectedListener {
            /**
             * Invoked when a user row is tapped.
             *
             * @param user the user that was selected
             */
            void onUserSelected(Users user);
        }

        private final List<Users> users;
        private final OnUserSelectedListener onUserSelectedListener;

        /**
         * @param users                  the live, filtered list backing the adapter
         * @param onUserSelectedListener callback to invoke when a row is tapped
         */
        UserPickerAdapter(List<Users> users, OnUserSelectedListener onUserSelectedListener) {
            this.users = users;
            this.onUserSelectedListener = onUserSelectedListener;
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public UserPickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_picker, parent, false);
            return new UserPickerViewHolder(view);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Displays the user's display name and available contact information (email and/or
         * phone). Falls back to "No contact info" when neither is available.
         */
        @Override
        public void onBindViewHolder(@NonNull UserPickerViewHolder holder, int position) {
            Users user = users.get(position);
            holder.tvName.setText(buildUserDisplayName(user));

            String email = valueOrEmpty(user.getEmail());
            String phone = valueOrEmpty(user.getPhoneNumber());
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

        /** {@inheritDoc} */
        @Override
        public int getItemCount() {
            return users.size();
        }

        /**
         * ViewHolder that caches references to the name and contact-detail TextViews for a
         * single user row in the picker list.
         */
        static class UserPickerViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvDetails;

            /**
             * @param itemView the inflated {@code item_user_picker} row view
             */
            UserPickerViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_user_picker_name);
                tvDetails = itemView.findViewById(R.id.tv_user_picker_details);
            }
        }
    }
}
