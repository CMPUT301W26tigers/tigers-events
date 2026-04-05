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

    /**
     * Constructs a RecipientAdapter.
     *
     * @param recipients A list of recipient maps, each expected to contain at least a {@code "name"} key.
     */
    public RecipientAdapter(List<Map<String, String>> recipients) {
        this.recipients = recipients;
    }

    /**
     * Inflates the recipient name item layout and wraps it in a {@link ViewHolder}.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (unused; only one type exists).
     * @return A new {@link ViewHolder} backed by the inflated item view.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipient_name, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds recipient data to the provided {@link ViewHolder}. Displays the recipient's name
     * prefixed with a bullet character, falling back to {@code "Unknown"} when the name is absent.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item within the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> recipient = recipients.get(position);
        String name = recipient.get("name");
        holder.tvName.setText("  \u2022  " + (name != null ? name : "Unknown"));
    }

    /**
     * Returns the total number of recipients managed by this adapter.
     * Guards against a {@code null} list by returning {@code 0}.
     *
     * @return The number of recipients, or {@code 0} if the list is {@code null}.
     */
    @Override
    public int getItemCount() {
        return recipients != null ? recipients.size() : 0;
    }

    /**
     * ViewHolder for a single recipient name row in the nested recipients list.
     * Holds a reference to the name text view defined in {@code item_recipient_name.xml}.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        /**
         * Constructs a ViewHolder and resolves the recipient name text view.
         *
         * @param itemView The inflated recipient name item view.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRecipientName);
        }
    }
}
