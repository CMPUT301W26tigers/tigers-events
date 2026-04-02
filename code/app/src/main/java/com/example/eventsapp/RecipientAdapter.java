package com.example.eventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

/**
 * Simple adapter that displays recipient names in a nested RecyclerView.
 */
public class RecipientAdapter extends RecyclerView.Adapter<RecipientAdapter.ViewHolder> {
    private final List<Map<String, String>> recipients;

    public RecipientAdapter(List<Map<String, String>> recipients) {
        this.recipients = recipients;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipient_name, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> recipient = recipients.get(position);
        String name = recipient.get("name");
        holder.tvName.setText("  \u2022  " + (name != null ? name : "Unknown"));
    }

    @Override
    public int getItemCount() {
        return recipients != null ? recipients.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecipientName);
        }
    }
}
