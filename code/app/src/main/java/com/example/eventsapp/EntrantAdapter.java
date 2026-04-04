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

import com.google.android.material.card.MaterialCardView;

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

    private final List<Entrant> entrants;
    private OnViewLocationListener onViewLocationListener;
    private Set<String> tempSelectedIds = new HashSet<>(); // Tracks local drafted lottery selections

    public void setOnViewLocationListener(OnViewLocationListener listener) {
        this.onViewLocationListener = listener;
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
        holder.tvName.setText(e.getName() != null && !e.getName().isEmpty() ? e.getName() : "Unknown");
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
        ColorStateList defaultActionTextColor;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.item_waitlist_root);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAction = itemView.findViewById(R.id.tv_action);
            tvActionSub = itemView.findViewById(R.id.tv_action_sub);
            ivPin = itemView.findViewById(R.id.iv_pin);
            ivMailIcon = itemView.findViewById(R.id.iv_mail_icon);
            defaultActionTextColor = tvAction.getTextColors();
        }
    }
}
