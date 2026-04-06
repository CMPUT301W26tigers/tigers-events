package com.example.eventsapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

/**
 * RecyclerView adapter used to display enrolled entrants.
 * Uses the shared {@link Entrant} model instead of the removed EnrolledEntrant class.
 */
public class EnrolledEntrantAdapter extends RecyclerView.Adapter<EnrolledEntrantAdapter.EnrolledViewHolder> {

    private final Context context;
    private final ArrayList<Entrant> entrants;

    /**
     * Constructs an EnrolledEntrantAdapter.
     *
     * @param context  The context used to inflate item layouts.
     * @param entrants The list of enrolled {@link Entrant} objects to display.
     */
    public EnrolledEntrantAdapter(Context context, ArrayList<Entrant> entrants) {
        this.context = context;
        this.entrants = entrants;
    }

    /**
     * Inflates the item layout for an enrolled entrant and wraps it in a {@link EnrolledViewHolder}.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (unused; only one type exists).
     * @return A new {@link EnrolledViewHolder} backed by the inflated item view.
     */
    @NonNull
    @Override
    public EnrolledViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_enrolled_entrant, parent, false);
        return new EnrolledViewHolder(view);
    }

    /**
     * Binds entrant data to the provided {@link EnrolledViewHolder}. Displays the entrant's name,
     * email, and status, falling back to {@code "ACCEPTED"} when no status is set.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item within the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EnrolledViewHolder holder, int position) {
        Entrant e = entrants.get(position);

        String nameText = e.getName() != null && !e.getName().trim().isEmpty() ? e.getName().trim() : "Unknown";
        holder.tvName.setText(nameText);

        // Default Avatar Initial state (clears old data for view recycling)
        String initial = nameText.equals("Unknown") ? "?" : nameText.substring(0, 1).toUpperCase();
        holder.tvAvatarInitial.setText(initial);
        holder.tvAvatarInitial.setVisibility(View.VISIBLE);
        holder.ivProfilePic.setVisibility(View.GONE);
        Glide.with(holder.itemView.getContext()).clear(holder.ivProfilePic);

        // Fetch Profile Picture dynamically
        String userId = e.getId();
        holder.itemView.setTag(userId); // Tag prevents image loading into the wrong row if scrolled fast
        if (userId != null && !userId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Ensure view hasn't been recycled for another entrant
                        if (userId.equals(holder.itemView.getTag()) && documentSnapshot.exists()) {
                            String url = documentSnapshot.getString("profilePictureUrl");
                            if (url != null && !url.isEmpty()) {
                                holder.ivProfilePic.setVisibility(View.VISIBLE);
                                holder.tvAvatarInitial.setVisibility(View.GONE);
                                Glide.with(holder.itemView.getContext())
                                        .load(url)
                                        .circleCrop()
                                        .into(holder.ivProfilePic);
                            }
                        }
                    });
        }

        holder.tvStatus.setText(e.getStatus() != null ? e.getStatus().name() : "ACCEPTED");
        holder.tvEmail.setText(e.getEmail() != null ? e.getEmail() : "");
    }

    /**
     * Returns the total number of enrolled entrants managed by this adapter.
     *
     * @return The size of the entrants list.
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * ViewHolder for a single enrolled entrant row.
     * Holds references to the name, email, and status text views
     * defined in {@code item_enrolled_entrant.xml}.
     */
    static class EnrolledViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvStatus;
        ImageView ivProfilePic;
        TextView tvAvatarInitial;

        /**
         * Constructs an EnrolledViewHolder and resolves all child view references.
         *
         * @param itemView The inflated item view for an enrolled entrant row.
         */
        public EnrolledViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivProfilePic = itemView.findViewById(R.id.iv_profile_pic);
            tvAvatarInitial = itemView.findViewById(R.id.tv_avatar_initial);
        }
    }
}
