package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {
    private static final String TAG = "ExploreFragment";
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private ExploreEventAdapter adapter;
    private ExploreEventAdapter searchAdapter;
    private RecyclerView rvEvents;
    private RecyclerView rvSearchResults;
    private ConstraintLayout normalContainer;
    private ConstraintLayout searchContainer;

    public ExploreFragment() { super(R.layout.fragment_explore); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvEvents = view.findViewById(R.id.rvEvents);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        normalContainer = view.findViewById(R.id.normalContainer);
        searchContainer = view.findViewById(R.id.searchContainer);

        // Main event list adapter
        adapter = new ExploreEventAdapter(allEvents, event -> navigateToDetail(view, event));
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(adapter);

        // Search results adapter
        searchAdapter = new ExploreEventAdapter(filteredEvents, event -> navigateToDetail(view, event));
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(searchAdapter);

        // Tap the search bar hint to switch to search mode
        view.findViewById(R.id.cardSearchNormal).setOnClickListener(v -> showSearchMode());

        // Back button exits search mode
        view.findViewById(R.id.btnBack).setOnClickListener(v -> hideSearchMode());

        // Search text filtering
        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadAllEvents();
    }

    private void navigateToDetail(View view, Event event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getId());
        Navigation.findNavController(view)
                .navigate(R.id.action_exploreFragment_to_eventDetailFragment, args);
    }

    private void showSearchMode() {
        normalContainer.setVisibility(View.GONE);
        searchContainer.setVisibility(View.VISIBLE);
        filteredEvents.clear();
        filteredEvents.addAll(allEvents);
        searchAdapter.notifyDataSetChanged();
    }

    private void hideSearchMode() {
        searchContainer.setVisibility(View.GONE);
        normalContainer.setVisibility(View.VISIBLE);
        EditText etSearch = requireView().findViewById(R.id.etSearch);
        etSearch.setText("");
    }

    private void filterEvents(String query) {
        filteredEvents.clear();
        if (query.isEmpty()) {
            filteredEvents.addAll(allEvents);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Event event : allEvents) {
                if (event.getName().toLowerCase().contains(lowerQuery)) {
                    filteredEvents.add(event);
                }
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void loadAllEvents() {
        CollectionReference eventsRef = FirebaseFirestore.getInstance().collection("events");
        eventsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error loading events", error);
                return;
            }
            if (value != null && isAdded()) {
                allEvents.clear();
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
                        allEvents.add(new Event(id, name, amount, description, posterUrl, sampleSize));
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private static class ExploreEventAdapter extends RecyclerView.Adapter<ExploreEventAdapter.ViewHolder> {
        private final List<Event> events;
        private final OnEventClickListener listener;

        interface OnEventClickListener {
            void onEventClick(Event event);
        }

        ExploreEventAdapter(List<Event> events, OnEventClickListener listener) {
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
