package com.example.eventsapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EnrolledEntrantAdapter extends RecyclerView.Adapter<EnrolledEntrantAdapter.EnrolledViewHolder> {

    private Context context;
    private ArrayList<EnrolledEntrant> entrants;

    public EnrolledEntrantAdapter(Context context, ArrayList<EnrolledEntrant> entrants) {
        this.context = context;
        this.entrants = entrants;
    }

    @NonNull
    @Override
    public EnrolledViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.enrolled_entrant, parent, false);
        return new EnrolledViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EnrolledViewHolder holder, int position) {
        EnrolledEntrant entrant = entrants.get(position);

        holder.tvName.setText(entrant.getName());
        holder.tvEmail.setText(entrant.getEmail());
        holder.tvStatus.setText(entrant.getStatus());
    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class EnrolledViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvStatus;

        public EnrolledViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}