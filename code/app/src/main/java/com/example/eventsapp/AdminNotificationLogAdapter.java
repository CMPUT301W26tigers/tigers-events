package com.example.eventsapp;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for displaying notification log entries in the admin notification management screen.
 * Each item is collapsible: collapsed shows organizer name and preview,
 * expanded shows full message and recipients list.
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.ViewHolder> {
    private final List<NotificationLogItem> logs;
    private final SparseBooleanArray expandedPositions = new SparseBooleanArray();

    public AdminNotificationLogAdapter(List<NotificationLogItem> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationLogItem log = logs.get(position);

        String orgName = log.getOrganizerName();
        holder.tvOrganizerName.setText(orgName != null && !orgName.isEmpty() ? orgName : "Unknown Organizer");

        String preview = buildPreview(log);
        holder.tvNotificationPreview.setText(preview);

        // Expanded content
        String fullMessage = "Event: " + safe(log.getEventName()) + "\n"
                + "Type: " + formatType(log.getType()) + "\n\n"
                + safe(log.getMessage());
        holder.tvNotificationFull.setText(fullMessage);

        List<String> recipientNames = log.getRecipientNames();
        holder.tvRecipientsHeader.setText("Recipients (" + recipientNames.size() + ")");

        RecipientAdapter recipientAdapter = new RecipientAdapter(
                log.getRecipients() != null ? log.getRecipients() : Collections.emptyList());
        holder.rvRecipients.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.rvRecipients.setAdapter(recipientAdapter);

        // Expand/collapse state
        boolean isExpanded = expandedPositions.get(position, false);
        holder.layoutExpanded.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpandArrow.setRotation(isExpanded ? 180f : 0f);

        holder.layoutCollapsed.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            boolean nowExpanded = !expandedPositions.get(pos, false);
            expandedPositions.put(pos, nowExpanded);
            notifyItemChanged(pos);
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    private String buildPreview(NotificationLogItem log) {
        String eventName = safe(log.getEventName());
        String title = safe(log.getTitle());
        if (!eventName.isEmpty() && !title.isEmpty()) {
            return eventName + " — " + title;
        }
        return !eventName.isEmpty() ? eventName : title;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    static String formatType(String type) {
        if (type == null) return "Unknown";
        switch (type) {
            case "invitation": return "Invitation";
            case "waitlisted": return "Waitlisted";
            case "not_selected": return "Not Selected";
            case "private_waitlist_invitation": return "Private Invite";
            case "co_organizer_invitation": return "Co-organizer Invite";
            default: return type;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutCollapsed;
        View layoutExpanded;
        TextView tvOrganizerName, tvNotificationPreview, tvNotificationFull, tvRecipientsHeader;
        ImageView ivExpandArrow;
        RecyclerView rvRecipients;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCollapsed = itemView.findViewById(R.id.layoutCollapsed);
            layoutExpanded = itemView.findViewById(R.id.layoutExpanded);
            tvOrganizerName = itemView.findViewById(R.id.tvOrganizerName);
            tvNotificationPreview = itemView.findViewById(R.id.tvNotificationPreview);
            tvNotificationFull = itemView.findViewById(R.id.tvNotificationFull);
            tvRecipientsHeader = itemView.findViewById(R.id.tvRecipientsHeader);
            ivExpandArrow = itemView.findViewById(R.id.ivExpandArrow);
            rvRecipients = itemView.findViewById(R.id.rvRecipients);
        }
    }
}
