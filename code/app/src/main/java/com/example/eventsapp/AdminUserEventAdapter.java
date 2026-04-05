package com.example.eventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter that lists events belonging to a specific user in the admin
 * user-detail screen. Each row shows the event name and date; tapping a row opens
 * the event details via {@link OnEventClickListener}.
 */
public class AdminUserEventAdapter extends RecyclerView.Adapter<AdminUserEventAdapter.ViewHolder> {

    /**
     * Callback invoked when an admin taps an event row in the user-detail screen.
     */
    public interface OnEventClickListener {
        /**
         * Called when an event row is clicked.
         *
         * @param event The {@link Event} represented by the tapped row.
         */
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    /**
     * Constructs an AdminUserEventAdapter.
     *
     * @param events   The list of {@link Event} objects to display.
     * @param listener Callback invoked when an event row is tapped; must not be {@code null}.
     */
    public AdminUserEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    /**
     * Inflates the event-row layout and wraps it in a {@link ViewHolder}.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (unused; only one type exists).
     * @return A new {@link ViewHolder} backed by the inflated row view.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user_event, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Binds event data to the provided {@link ViewHolder}. Displays the event name and formatted
     * date, and attaches a click listener that delegates to {@link OnEventClickListener}.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item within the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvName.setText(event.getName());
        holder.tvDate.setText(event.getFormattedEventDate());
        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    /**
     * Returns the total number of events managed by this adapter.
     *
     * @return The size of the events list.
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for a single event row in the admin user-detail screen.
     * Holds references to the event name and date text views defined in
     * {@code item_admin_user_event.xml}.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate;

        /**
         * Constructs a ViewHolder and resolves all child view references.
         *
         * @param itemView The inflated event row view.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEventItemName);
            tvDate = itemView.findViewById(R.id.tvEventItemDate);
        }
    }
}
