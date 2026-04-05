package com.example.eventsapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        Entrant entrant = entrants.get(position);
        holder.tvName.setText(entrant.getName());
        holder.tvEmail.setText(entrant.getEmail());
        holder.tvStatus.setText(entrant.getStatus() != null ? entrant.getStatus().name() : "ACCEPTED");
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
        }
    }
}
