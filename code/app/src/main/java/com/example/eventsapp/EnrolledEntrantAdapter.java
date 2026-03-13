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
 * Adapter class for a {@link RecyclerView} that displays a list of {@link EnrolledEntrant} objects.
 * This adapter manages the creation and binding of view holders for entrants enrolled in an event.
 */
public class EnrolledEntrantAdapter extends RecyclerView.Adapter<EnrolledEntrantAdapter.EnrolledViewHolder> {

    private Context context;
    private ArrayList<EnrolledEntrant> entrants;

    /**
     * Constructs an EnrolledEntrantAdapter.
     *
     * @param context The context in which the adapter is operating.
     * @param entrants The list of enrolled entrants to be displayed.
     */
    public EnrolledEntrantAdapter(Context context, ArrayList<EnrolledEntrant> entrants) {
        this.context = context;
        this.entrants = entrants;
    }

    /**
     * Called when RecyclerView needs a new {@link EnrolledViewHolder} of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EnrolledViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EnrolledViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.enrolled_entrant, parent, false);
        return new EnrolledViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link EnrolledViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EnrolledViewHolder holder, int position) {
        EnrolledEntrant entrant = entrants.get(position);

        holder.tvName.setText(entrant.getName());
        holder.tvEmail.setText(entrant.getEmail());
        holder.tvStatus.setText(entrant.getStatus());
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
