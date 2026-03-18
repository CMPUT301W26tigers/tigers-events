package com.example.eventsapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * A custom {@link ArrayAdapter} for displaying a list of {@link Event} objects in a ListView.
 * This adapter inflates a custom layout for each event item and binds the event data to the views.
 */
public class EventArrayAdapter extends ArrayAdapter<Event> {
    private ArrayList<Event> events;
    private Context context;

    /**
     * Constructs an EventArrayAdapter.
     *
     * @param context The current context.
     * @param events The list of events to be displayed.
     */
    public EventArrayAdapter(Context context, ArrayList<Event> events){
        super(context, 0, events);
        this.events = events;
        this.context = context;
    }

    /**
     * Provides a view for an AdapterView (ListView, GridView, etc.).
     *
     * @param position The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context).inflate(R.layout.layout_event, parent, false);
        }

        Event event = events.get(position);
        TextView eventName = view.findViewById(R.id.textEventName);
        TextView eventAmount = view.findViewById(R.id.textEventAmount);

        eventName.setText(event.getName());
        eventAmount.setText(String.valueOf(event.getAmount()));

        return view;
    }
}
