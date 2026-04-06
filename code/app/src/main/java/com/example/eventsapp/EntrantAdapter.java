package com.example.eventsapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter class for a {@link RecyclerView} that displays a list of {@link Entrant} objects.
 * This adapter is typically used to show entrants on a waitlist or invited list.
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.ViewHolder> {

    /**
     * Opens the map focused on this entrant (when they have coordinates).
     */
    public interface OnViewLocationListener {
        void onViewLocation(Entrant entrant);
    }

    /**
     +     * Triggers when the organizer cancels an invited entrant.
     +     */
    public interface OnCancelEntrantListener {
        void onCancel(Entrant entrant);
    }

    private final List<Entrant> entrants;
    private OnViewLocationListener onViewLocationListener;
    private OnCancelEntrantListener onCancelEntrantListener;
    private Set<String> tempSelectedIds = new HashSet<>(); // Tracks local drafted lottery selections

    /**
     * Registers a listener that is invoked when the user taps the map-pin icon on an entrant row.
     *
     * @param listener The callback to invoke, or {@code null} to remove the current listener.
     */
    public void setOnViewLocationListener(OnViewLocationListener listener) {
        this.onViewLocationListener = listener;
    }

    /**
     * Registers a listener that is invoked when the organizer cancels an invited entrant.
     *
     * @param listener The callback to invoke, or {@code null} to remove the current listener.
     */
    public void setOnCancelEntrantListener(OnCancelEntrantListener listener) {
        this.onCancelEntrantListener = listener;
    }


    private static String statusLabel(Entrant.Status status) {
        if (status == Entrant.Status.ACCEPTED) return "Accepted";
        if (status == Entrant.Status.INVITED) return "Invited";
        if (status == Entrant.Status.APPLIED) return "Waitlist";
        if (status == Entrant.Status.DECLINED) return "Declined";
        if (status == Entrant.Status.CANCELLED) return "Cancelled";
        return "Unknown";
    }

    /**
     * Constructs an EntrantAdapter.
     *
     * @param entrants The list of entrants to be displayed.
     */
    public EntrantAdapter(List<Entrant> entrants) {
        this.entrants = entrants;
    }

    /**
     * Updates the locally drafted entrants selected by the lottery.
     *
     * @param tempSelectedIds The IDs of drafted entrants.
     */
    public void setTempSelectedIds(Set<String> tempSelectedIds) {
        this.tempSelectedIds = tempSelectedIds;
        notifyDataSetChanged();
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waitlist_entrant, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

        holder.tvAction.setText(getStatusLabel(e.getStatus()));
        holder.tvActionSub.setText(e.getEmail() != null ? e.getEmail() : "");

        boolean isDrafted = tempSelectedIds.contains(e.getId());

        // Apply dynamic styling based on drafted lottery state
        if (isDrafted) {
            holder.tvAction.setText("SELECTED (Draft)");
            holder.tvAction.setTextColor(Color.parseColor("#4CAF50")); // Green text
            holder.cardView.setStrokeColor(Color.parseColor("#4CAF50")); // Green border
            holder.cardView.setStrokeWidth(4);
            holder.ivMailIcon.setVisibility(View.GONE);
        } else {
            holder.tvAction.setText(getStatusLabel(e.getStatus()));
            holder.tvAction.setTextColor(holder.defaultActionTextColor); // Revert to original color
            holder.cardView.setStrokeWidth(0); // Remove border

            // Show mail icon if formally invited
            if (e.getStatus() == Entrant.Status.INVITED) {
                holder.ivMailIcon.setVisibility(View.VISIBLE);
            } else {
                holder.ivMailIcon.setVisibility(View.GONE);
            }

            // Show cancel icon only if formally invited and not drafted
            if (e.getStatus() == Entrant.Status.INVITED) {
                holder.ivCancel.setVisibility(View.VISIBLE);
                holder.ivCancel.setOnClickListener(v -> {
                    if (onCancelEntrantListener != null) {
                        onCancelEntrantListener.onCancel(e);
                    }
                });
            } else {
                holder.ivCancel.setVisibility(View.GONE);
                holder.ivCancel.setOnClickListener(null);
            }
        }

        if (e.hasLocation()) {
            holder.ivPin.setVisibility(View.VISIBLE);
            holder.ivPin.setOnClickListener(v -> {
                if (onViewLocationListener != null) {
                    onViewLocationListener.onViewLocation(e);
                }
            });
        } else {
            holder.ivPin.setVisibility(View.INVISIBLE);
            holder.ivPin.setOnClickListener(null);
        }
    }

    private String getStatusLabel(Entrant.Status status) {
        if (status == null) {
            return "Pending";
        }
        switch (status) {
            case PRIVATE_INVITED:
                return "Invited";
            case APPLIED:
                return "Waitlisted";
            case ACCEPTED:
                return "Accepted";
            case INVITED:
                return "Invited";
            case DECLINED:
                return "Declined";
            case CANCELLED:
                return "Cancelled";
            default:
                return "Pending";
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of entrants in the list.
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * ViewHolder class for entrant items.
     * Holds references to the UI components for each list item in the waitlist.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvName;
        TextView tvAction;
        TextView tvActionSub;
        ImageView ivPin;
        ImageView ivMailIcon;
        ImageView ivCancel;
        ImageView ivProfilePic;
        TextView tvAvatarInitial;

        ColorStateList defaultActionTextColor;

        /**
         * Constructs a ViewHolder and resolves all child view references from the item layout.
         * Captures the default text color of {@code tvAction} so it can be restored after
         * temporary lottery-draft highlighting is cleared.
         *
         * @param itemView The inflated item view for a waitlist entrant row.
         */
        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.item_waitlist_root);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAction = itemView.findViewById(R.id.tv_action);
            tvActionSub = itemView.findViewById(R.id.tv_action_sub);
            ivPin = itemView.findViewById(R.id.iv_pin);
            ivMailIcon = itemView.findViewById(R.id.iv_mail_icon);
            ivCancel = itemView.findViewById(R.id.iv_cancel_entrant);
            ivProfilePic = itemView.findViewById(R.id.iv_profile_pic);
            tvAvatarInitial = itemView.findViewById(R.id.tv_avatar_initial);
            defaultActionTextColor = tvAction.getTextColors();
        }
    }
}
