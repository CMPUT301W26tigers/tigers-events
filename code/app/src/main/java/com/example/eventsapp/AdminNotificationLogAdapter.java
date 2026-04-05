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

    /**
     * Constructs an AdminNotificationLogAdapter.
     *
     * @param logs The list of {@link NotificationLogItem} entries to display.
     */
    public AdminNotificationLogAdapter(List<NotificationLogItem> logs) {
        this.logs = logs;
    }

    /**
     * Inflates the notification log item layout and wraps it in a {@link ViewHolder}.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (unused; only one type exists).
     * @return A new {@link ViewHolder} backed by the inflated item view.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification_log, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds a {@link NotificationLogItem} to the provided {@link ViewHolder}. Sets the organizer
     * name, a short collapsed preview, and the full expanded message with a nested recipient list.
     * Restores the expand/collapse state and wires the toggle click handler.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item within the data set.
     */
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

    /**
     * Returns the total number of notification log entries managed by this adapter.
     *
     * @return The size of the logs list.
     */
    @Override
    public int getItemCount() {
        return logs.size();
    }

    /**
     * Builds a one-line preview string shown in the collapsed state of a log entry.
     * Combines event name and notification title when both are present.
     *
     * @param log The log entry to summarise.
     * @return A non-null preview string.
     */
    private String buildPreview(NotificationLogItem log) {
        String eventName = safe(log.getEventName());
        String title = safe(log.getTitle());
        if (!eventName.isEmpty() && !title.isEmpty()) {
            return eventName + " — " + title;
        }
        return !eventName.isEmpty() ? eventName : title;
    }

    /**
     * Returns the value unchanged, or an empty string if the value is {@code null}.
     *
     * @param value The string to guard.
     * @return A non-null string.
     */
    private String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * Converts a raw notification type key into a human-readable label shown in the expanded view.
     *
     * @param type The raw type string stored in Firestore (e.g. {@code "invitation"}).
     * @return A display label, or the original type string if the key is unrecognised.
     */
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

    /**
     * ViewHolder for a single notification log entry.
     * Manages two layout regions — a collapsed summary and an expanded detail panel —
     * along with a nested {@link RecyclerView} for the recipient list.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutCollapsed;
        View layoutExpanded;
        TextView tvOrganizerName, tvNotificationPreview, tvNotificationFull, tvRecipientsHeader;
        ImageView ivExpandArrow;
        RecyclerView rvRecipients;

        /**
         * Constructs a ViewHolder and resolves all child view references.
         *
         * @param itemView The inflated notification log item view.
         */
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
