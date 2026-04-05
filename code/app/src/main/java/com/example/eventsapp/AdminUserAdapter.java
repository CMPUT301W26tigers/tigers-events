package com.example.eventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * RecyclerView adapter that displays all registered users in the admin user-management screen.
 * Each card shows the user's display name, account role, and avatar (profile photo or
 * initial letter fallback). Tapping a card notifies the host via {@link OnUserClickListener}.
 */
public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    /**
     * Callback invoked when an admin taps a user card.
     */
    public interface OnUserClickListener {
        /**
         * Called when the user card at the given position is clicked.
         *
         * @param user The {@link Users} object represented by the tapped card.
         */
        void onUserClick(Users user);
    }

    private final List<Users> users;
    private final OnUserClickListener listener;

    /**
     * Constructs an AdminUserAdapter.
     *
     * @param users    The list of {@link Users} to display.
     * @param listener Callback invoked when a user card is tapped; must not be {@code null}.
     */
    public AdminUserAdapter(List<Users> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    /**
     * Inflates the admin user card layout and wraps it in a {@link ViewHolder}.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (unused; only one type exists).
     * @return A new {@link ViewHolder} backed by the inflated card view.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user_card, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds user data to the provided {@link ViewHolder}. Resolves a display name from
     * {@code name}, then {@code firstName + lastName}, then falls back to {@code "Unknown"}.
     * Loads the profile picture via Glide when available; otherwise shows the first initial.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item within the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = users.get(position);

        String displayName = user.getName();
        if (displayName == null || displayName.isEmpty()) {
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            displayName = (first + " " + last).trim();
        }
        if (displayName.isEmpty()) displayName = "Unknown";

        holder.tvUserName.setText(displayName);
        holder.tvUserRole.setText(user.getAccountType() != null ? user.getAccountType() : "User");

        String picUrl = user.getProfilePictureUrl();
        if (picUrl != null && !picUrl.isEmpty()) {
            holder.ivAvatarImage.setVisibility(View.VISIBLE);
            holder.tvAvatarLetter.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext()).load(picUrl).circleCrop().into(holder.ivAvatarImage);
        } else {
            holder.ivAvatarImage.setVisibility(View.GONE);
            holder.tvAvatarLetter.setVisibility(View.VISIBLE);
            holder.tvAvatarLetter.setText(displayName.substring(0, 1).toUpperCase());
        }

        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    /**
     * Returns the total number of users managed by this adapter.
     *
     * @return The size of the users list.
     */
    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * ViewHolder for a single admin user card.
     * Holds references to the name, role, avatar image, and avatar-letter fallback views
     * defined in {@code item_admin_user_card.xml}.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserRole, tvAvatarLetter;
        ImageView ivAvatarImage;

        /**
         * Constructs a ViewHolder and resolves all child view references.
         *
         * @param itemView The inflated admin user card view.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
            ivAvatarImage = itemView.findViewById(R.id.ivAvatarImage);
        }
    }
}
