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
 * RecyclerView adapter used to display cancelled and declined entrants.
 * Binds Entrant objects to the item_cancelled_entrant.xml layout.
 */
public class CancelledEntrantAdapter extends RecyclerView.Adapter<CancelledEntrantAdapter.CancelledViewHolder> {

    private final Context context;
    private final ArrayList<Entrant> entrants;

    /**
     * Constructs a CancelledEntrantAdapter.
     *
     * @param context  The context used to inflate item layouts.
     * @param entrants The list of cancelled or declined {@link Entrant} objects to display.
     */
    public CancelledEntrantAdapter(Context context, ArrayList<Entrant> entrants) {
        this.context = context;
        this.entrants = entrants;
    }

    /**
     * Inflates the cancelled-entrant item layout and wraps it in a {@link CancelledViewHolder}.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (unused; only one type exists).
     * @return A new {@link CancelledViewHolder} backed by the inflated item view.
     */
    @NonNull
    @Override
    public CancelledViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cancelled_entrant, parent, false);
        return new CancelledViewHolder(view);
    }

    /**
     * Binds entrant data to the provided {@link CancelledViewHolder}. Displays the entrant's
     * name and email, and sets a human-readable status label distinguishing
     * {@link Entrant.Status#DECLINED} from {@link Entrant.Status#CANCELLED}.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item within the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull CancelledViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);

        holder.tvName.setText(entrant.getName() != null && !entrant.getName().isEmpty() ? entrant.getName() : "Unknown");
        holder.tvEmail.setText(entrant.getEmail() != null ? entrant.getEmail() : "No email");

        // Formally label whether they cancelled or declined
        if (entrant.getStatus() == Entrant.Status.DECLINED) {
            holder.tvStatus.setText("Declined");
        } else if (entrant.getStatus() == Entrant.Status.CANCELLED) {
            holder.tvStatus.setText("Cancelled");
        } else {
            holder.tvStatus.setText("Unknown");
        }
    }

    /**
     * Returns the total number of cancelled/declined entrants managed by this adapter.
     *
     * @return The size of the entrants list.
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * ViewHolder for a single cancelled or declined entrant row.
     * Holds references to the name, email, and status text views
     * defined in {@code item_cancelled_entrant.xml}.
     */
    static class CancelledViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvStatus;

        /**
         * Constructs a CancelledViewHolder and resolves all child view references.
         *
         * @param itemView The inflated item view for a cancelled entrant row.
         */
        public CancelledViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}