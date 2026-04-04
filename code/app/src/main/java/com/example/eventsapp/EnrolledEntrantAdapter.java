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
 * RecyclerView adapter used to display enrolled entrants
 *
 * The adapter binds EnrolledEntrant objects to the item_enrolled_entrant.xml
 *
 * Displays: participant name, participant email, enrollment status
 *
 * Used by EnrolledFragment to render the list of confirmed participants
 */
public class EnrolledEntrantAdapter extends RecyclerView.Adapter<EnrolledEntrantAdapter.EnrolledViewHolder> {

    private Context context;
    private ArrayList<EnrolledEntrant> entrants;

    /**
     * Creates a new adapter for enrolled entrants
     *
     * @param context application context
     * @param entrants list of enrolled entrants
     */
    public EnrolledEntrantAdapter(Context context, ArrayList<EnrolledEntrant> entrants) {
        this.context = context;
        this.entrants = entrants;
    }


    @NonNull
    /**
     * Creates a new ViewHolder for an entrant row.
     *
     * @param parent parent view group
     * @param viewType type of view
     * @return a new EnrolledViewHolder
     */
    @Override
    public EnrolledViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_enrolled_entrant, parent, false);
        return new EnrolledViewHolder(view);
    }

    /**
     * Binds entrant data to the row.
     *
     * @param holder ViewHolder for the row
     * @param position position of the entrant in the list
     */
    @Override
    public void onBindViewHolder(@NonNull EnrolledViewHolder holder, int position) {
        EnrolledEntrant entrant = entrants.get(position);

        holder.tvName.setText(entrant.getName());
        holder.tvEmail.setText(entrant.getEmail());
        holder.tvStatus.setText(entrant.getStatus());
    }

    /**
     * Returns the number of enrolled entrants in the list.
     *
     * @return number of entrants
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * ViewHolder class for enrolled entrant items.
     * Holds references to the UI components for each list item.
     */
    static class EnrolledViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvStatus;

        /**
         * Constructs an EnrolledViewHolder.
         *
         * @param itemView The view representing a single list item.
         */
        public EnrolledViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
