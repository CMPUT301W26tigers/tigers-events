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

import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EventsFragment extends Fragment {
    private static final String TAG = "EventsFragment";

    private final List<Event> eventList = new ArrayList<>();
    private EventCardAdapter adapter;
    private RecyclerView rvMyEvents;
    private LinearLayout emptyStateContainer;
    private MaterialButtonToggleGroup toggleEventType;

    public EventsFragment() { super(R.layout.fragment_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMyEvents = view.findViewById(R.id.rvMyEvents);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        toggleEventType = view.findViewById(R.id.toggleEventType);
        ChipGroup chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        adapter = new EventCardAdapter(eventList, event -> {
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());
            Navigation.findNavController(view)
                    .navigate(R.id.action_eventsFragment_to_eventDetailFragment, args);
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
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            eventList.clear();
            adapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        String userId = currentUser.getId();
        int checkedId = toggleEventType.getCheckedButtonId();

        if (checkedId == R.id.btnCreated) {
            loadCreatedEvents(userId);
        } else {
            loadRegisteredEvents(userId);
        }
    }

    /**
     * Load events where createdBy == current user's ID.
     */
    private void loadCreatedEvents(String userId) {
        FirebaseFirestore.getInstance().collection("events")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    eventList.clear();
                    for (QueryDocumentSnapshot snapshot : querySnapshot) {
                        Event event = parseEvent(snapshot);
                        if (event != null) {
                            eventList.add(event);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load created events", e);
                });
    }

    /**
     * Load events where the current user has an entrant doc in the entrants subcollection.
     */
    private void loadRegisteredEvents(String userId) {
        FirebaseFirestore.getInstance().collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    eventList.clear();

                    if (querySnapshot.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        return;
                    }

                    List<QueryDocumentSnapshot> allDocs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        allDocs.add(doc);
                    }

                    AtomicInteger remaining = new AtomicInteger(allDocs.size());

                    for (QueryDocumentSnapshot snapshot : allDocs) {
                        String eventId = snapshot.getString("id");
                        if (eventId == null) eventId = snapshot.getId();

                        FirebaseFirestore.getInstance()
                                .collection("events").document(eventId)
                                .collection("entrants")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener(entrantSnapshot -> {
                                    if (!isAdded()) return;
                                    if (!entrantSnapshot.isEmpty()) {
                                        Event event = parseEvent(snapshot);
                                        if (event != null) {
                                            eventList.add(event);
                                        }
                                    }
                                    if (remaining.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    Log.e(TAG, "Failed to check entrants", e);
                                    if (remaining.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load events", e);
                });
    }

    private Event parseEvent(QueryDocumentSnapshot snapshot) {
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
            return new Event(id, name, amount, description, posterUrl, sampleSize);
        }
        return null;
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
