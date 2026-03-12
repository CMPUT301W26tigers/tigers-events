package com.example.eventsapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {
    private final List<Event> eventList = new ArrayList<>();
    private EventCardAdapter adapter;
    private RecyclerView rvMyEvents;
    private LinearLayout emptyStateContainer;

    public EventsFragment() { super(R.layout.fragment_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMyEvents = view.findViewById(R.id.rvMyEvents);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        MaterialButtonToggleGroup toggleEventType = view.findViewById(R.id.toggleEventType);
        ChipGroup chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        adapter = new EventCardAdapter(eventList, event -> {
            EventDialogFragment.newInstance(event)
                    .show(requireActivity().getSupportFragmentManager(), "Event Details");
        });
        rvMyEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyEvents.setAdapter(adapter);

        loadEvents();

        toggleEventType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                loadEvents();
            }
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            loadEvents();
        });
    }

    private void loadEvents() {
        CollectionReference eventsRef = FirebaseFirestore.getInstance().collection("events");
        eventsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("EventsFragment", error.toString());
                return;
            }
            if (value != null && isAdded()) {
                eventList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getString("id");
                    String name = snapshot.getString("name");
                    Long amountLong = snapshot.getLong("amount");
                    int amount = (amountLong != null) ? amountLong.intValue() : 0;
                    String description = snapshot.getString("description");
                    String posterUrl = snapshot.getString("posterUrl");
                    Long sampleLong = snapshot.getLong("sampleSize");
                    int sampleSize = (sampleLong != null) ? sampleLong.intValue() : 0;
                    if (id == null) id = snapshot.getId();
                    if (name == null) name = "";
                    if (description == null) description = "";
                    if (posterUrl == null) posterUrl = "";

                    if (amount != 0) {
                        eventList.add(new Event(id, name, amount, description, posterUrl, sampleSize));
                    }
                }
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (eventList.isEmpty()) {
            rvMyEvents.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            rvMyEvents.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private static class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.ViewHolder> {
        private final List<Event> events;
        private final OnEventClickListener listener;

        interface OnEventClickListener {
            void onEventClick(Event event);
        }

        EventCardAdapter(List<Event> events, OnEventClickListener listener) {
            this.events = events;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Event event = events.get(position);
            holder.tvEventName.setText(event.getName());
            holder.tvEventHost.setText(event.getDescription());
            holder.tvAvatarLetter.setText(
                    event.getName().isEmpty() ? "?" : String.valueOf(event.getName().charAt(0)).toUpperCase()
            );
            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate, tvEventTime;
            ImageView ivThumb;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEventName = itemView.findViewById(R.id.tvEventName);
                tvEventHost = itemView.findViewById(R.id.tvEventHost);
                tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
                tvEventDate = itemView.findViewById(R.id.tvEventDate);
                tvEventTime = itemView.findViewById(R.id.tvEventTime);
                ivThumb = itemView.findViewById(R.id.ivThumb);
            }
        }
    }
}
