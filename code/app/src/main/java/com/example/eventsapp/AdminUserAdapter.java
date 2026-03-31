package com.example.eventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(Users user);
    }

    private final List<Users> users;
    private final OnUserClickListener listener;

    public AdminUserAdapter(List<Users> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user_card, parent, false);
        return new ViewHolder(v);
    }

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

        String initial = displayName.substring(0, 1).toUpperCase();
        holder.tvAvatarLetter.setText(initial);

        final String finalName = displayName;
        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserRole, tvAvatarLetter;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
        }
    }
}
