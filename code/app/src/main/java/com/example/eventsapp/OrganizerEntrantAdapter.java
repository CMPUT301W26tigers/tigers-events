package com.example.eventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

/**
 * Adapter class for managing and displaying the list of entrants in the organizer's waitlist view.
 * Handles the mapping of entrant data to the UI, including their current waitlist status,
 * and provides interaction through remove and replace buttons.
 */
public class OrganizerEntrantAdapter extends RecyclerView.Adapter<OrganizerEntrantAdapter.ViewHolder> {

    private final List<Entrant> entrants;
    private final OnEntrantActionListener listener;

    /**
     * Interface definition for callbacks to be invoked when an action is taken on an entrant.
     */
    public interface OnEntrantActionListener {
        /**
         * Called when the organizer clicks the remove button for a specific entrant.
         *
         * @param entrant The entrant to be removed.
         */
        void onRemove(Entrant entrant);

        /**
         * Called when the organizer clicks the replace button for a specific entrant.
         *
         * @param entrant The entrant to be replaced.
         */
        void onReplace(Entrant entrant);
    }

    /**
     * Constructs a new OrganizerEntrantAdapter.
     *
     * @param entrants The list of entrants to be displayed.
     * @param listener The listener for handling user interaction with the entrant items.
     */
    public OrganizerEntrantAdapter(List<Entrant> entrants, OnEntrantActionListener listener) {
        this.entrants = entrants;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_entrant, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * Updates the contents of the {@link ViewHolder} to reflect the entrant at the given position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entrant e = entrants.get(position);
        holder.tvName.setText(e.getName() != null && !e.getName().isEmpty() ? e.getName() : "Unknown");

        // Map statusCode to readable text
        String statusText;
        switch (e.getStatusCode()) {
            case 0: statusText = "In Pool (Uninvited)"; break;
            case 1: statusText = "Invited (Unseen)"; break;
            case 2: statusText = "Accepted"; break;
            case 3: statusText = "Rejected/Cancelled"; break;
            default: statusText = "Unknown"; break;
        }
        holder.tvStatus.setText("Status: " + statusText);

        holder.btnRemove.setOnClickListener(v -> listener.onRemove(e));
        holder.btnReplace.setOnClickListener(v -> listener.onReplace(e));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of entrants.
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * ViewHolder class for the entrant list items.
     * Caches references to the UI components to avoid repeated `findViewById` calls.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        MaterialButton btnRemove, btnReplace;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The View containing the layout for a single entrant item.
         */
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_entrant_name);
            tvStatus = itemView.findViewById(R.id.tv_entrant_status);
            btnRemove = itemView.findViewById(R.id.btn_remove);
            btnReplace = itemView.findViewById(R.id.btn_replace);
        }
    }
}