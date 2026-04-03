package com.example.eventsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.BundleCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminUserDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_USER = "user";

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public interface OnAccountDeletedListener {
        void onAccountDeleted(String userId);
    }

    private OnEventClickListener eventClickListener;
    private OnAccountDeletedListener deleteListener;

    public static AdminUserDetailBottomSheet newInstance(Users user) {
        AdminUserDetailBottomSheet sheet = new AdminUserDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_USER, user);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.eventClickListener = listener;
    }

    public void setOnAccountDeletedListener(OnAccountDeletedListener listener) {
        this.deleteListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user_detail_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Users user = BundleCompat.getSerializable(requireArguments(), ARG_USER, Users.class);
        if (user == null) return;

        // Resolve display name
        String displayName = user.getName();
        if (displayName == null || displayName.isEmpty()) {
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            displayName = (first + " " + last).trim();
        }
        if (displayName.isEmpty()) displayName = "Unknown";

        // Avatar: show profile picture if available, otherwise show initial
        TextView tvAvatarLetter = view.findViewById(R.id.tvDetailAvatarLetter);
        ImageView ivDetailAvatarImage = view.findViewById(R.id.ivDetailAvatarImage);
        String picUrl = user.getProfilePictureUrl();
        if (picUrl != null && !picUrl.isEmpty()) {
            ivDetailAvatarImage.setVisibility(View.VISIBLE);
            tvAvatarLetter.setVisibility(View.GONE);
            Glide.with(this).load(picUrl).circleCrop().into(ivDetailAvatarImage);
        } else {
            ivDetailAvatarImage.setVisibility(View.GONE);
            tvAvatarLetter.setVisibility(View.VISIBLE);
            tvAvatarLetter.setText(displayName.substring(0, 1).toUpperCase());
        }

        // Avatar tap → delete profile picture
        final String finalDisplayName = displayName;
        view.findViewById(R.id.avatarContainer).setOnClickListener(v ->
                showDeleteProfilePicConfirmation(user, finalDisplayName)
        );

        // Name
        ((TextView) view.findViewById(R.id.tvDetailName)).setText(displayName);

        // Account type badge (top row)
        String accountType = user.getAccountType() != null ? user.getAccountType() : "User";
        ((TextView) view.findViewById(R.id.tvDetailAccountType)).setText(accountType);

        // Email
        String email = user.getEmail() != null ? user.getEmail() : "—";
        ((TextView) view.findViewById(R.id.tvDetailEmail)).setText(email);

        // Phone
        String phone = user.getPhoneNumber();
        ((TextView) view.findViewById(R.id.tvDetailPhone)).setText(
                (phone != null && !phone.isEmpty()) ? phone : "—"
        );

        // Account type (full field)
        ((TextView) view.findViewById(R.id.tvDetailAccountTypeFull)).setText(accountType);

        // Events created list
        RecyclerView rvEvents = view.findViewById(R.id.rvDetailEvents);
        TextView tvNoEvents = view.findViewById(R.id.tvDetailNoEvents);
        List<Event> createdEvents = new ArrayList<>();

        AdminUserEventAdapter eventAdapter = new AdminUserEventAdapter(createdEvents, event -> {
            if (eventClickListener != null) {
                dismiss();
                eventClickListener.onEventClick(event);
            }
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(eventAdapter);

        // Fetch events where createdBy == user.getId()
        if (user.getId() != null) {
            FirebaseFirestore.getInstance()
                    .collection("events")
                    .whereEqualTo("createdBy", user.getId())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String id = doc.getString("id");
                            String name = doc.getString("name");
                            Long amountLong = doc.getLong("amount");
                            int amount = amountLong != null ? amountLong.intValue() : 1;
                            String regStart = doc.getString("registration_start");
                            String regEnd = doc.getString("registration_end");
                            String eventDate = doc.getString("event_date");
                            String description = doc.getString("description");
                            String posterUrl = doc.getString("posterUrl");
                            Long sampleLong = doc.getLong("sampleSize");
                            int sampleSize = sampleLong != null ? sampleLong.intValue() : 0;

                            if (id == null) id = doc.getId();
                            if (name == null) name = "";
                            if (regStart == null) regStart = "";
                            if (regEnd == null) regEnd = "";
                            if (eventDate == null) eventDate = "";
                            if (description == null) description = "";
                            if (posterUrl == null) posterUrl = "";

                            if (amount != 0) {
                                Event e = new Event(id, name, amount, regStart, regEnd, eventDate, description, posterUrl, sampleSize);
                                e.setHostId(user.getId());
                                createdEvents.add(e);
                            }
                        }
                        eventAdapter.notifyDataSetChanged();
                        if (createdEvents.isEmpty()) {
                            tvNoEvents.setVisibility(View.VISIBLE);
                            rvEvents.setVisibility(View.GONE);
                        } else {
                            tvNoEvents.setVisibility(View.GONE);
                            rvEvents.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            tvNoEvents.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        }

        // Delete account button
        MaterialButton btnDelete = view.findViewById(R.id.btnDeleteAccount);
        btnDelete.setOnClickListener(v -> showDeleteConfirmation(user));
    }

    private void showDeleteProfilePicConfirmation(Users user, String displayName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile Picture")
                .setMessage("Remove the profile picture for " + displayName + "? Their avatar will show their initial instead.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfilePicture(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProfilePicture(Users user) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getId())
                .update("profilePictureUrl", "")
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(requireContext(), "Profile picture removed", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to remove profile picture", Toast.LENGTH_SHORT).show()
                );
    }

    private void showDeleteConfirmation(Users user) {
        String displayName = user.getName();
        if (displayName == null || displayName.isEmpty()) {
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            displayName = (first + " " + last).trim();
        }
        if (displayName.isEmpty()) displayName = "this user";

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete " + displayName + "'s account? This will also delete all events they created. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAndEvents(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUserAndEvents(Users user) {
        String userId = user.getId();

        EventCleanupHelper.deleteUserCompletely(userId,
                () -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                    if (deleteListener != null) deleteListener.onAccountDeleted(userId);
                    dismiss();
                },
                e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
                }
        );
    }
}
