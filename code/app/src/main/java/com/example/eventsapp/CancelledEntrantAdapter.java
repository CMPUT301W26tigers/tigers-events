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

    public CancelledEntrantAdapter(Context context, ArrayList<Entrant> entrants) {
        this.context = context;
        this.entrants = entrants;
    }

    @NonNull
    @Override
    public CancelledViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cancelled_entrant, parent, false);
        return new CancelledViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class CancelledViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvStatus;

        public CancelledViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}