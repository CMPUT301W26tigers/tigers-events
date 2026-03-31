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

/**
 * A fragment that allows users to explore and search for all available events.
 * It features a main list of events and a search mode with real-time filtering.
 */
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

    /**
     * Default constructor for ExploreFragment.
     * Uses the layout R.layout.fragment_explore.
     */
    public ExploreFragment() { super(R.layout.fragment_explore); }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned. Initializes the UI components, sets up RecyclerViews for both normal and search modes,
     * and sets up the search bar with filtering logic.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Clean up expired events on load
        EventCleanupHelper.cleanupExpiredEvents();

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

    /**
     * Navigates to the {@link EventDetailFragment} for the selected event.
     *
     * @param view The current view.
     * @param event The event object to display details for.
     */
    private void navigateToDetail(View view, Event event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getId());
        Navigation.findNavController(view)
                .navigate(R.id.action_exploreFragment_to_eventDetailFragment, args);
    }

    /**
     * Switches the UI to search mode, showing the search container and hiding the normal list.
     */
    private void showSearchMode() {
        normalContainer.setVisibility(View.GONE);
        searchContainer.setVisibility(View.VISIBLE);
        filteredEvents.clear();
        filteredEvents.addAll(allEvents);
        searchAdapter.notifyDataSetChanged();
    }

    /**
     * Exits search mode, showing the normal list and clearing the search input.
     */
    private void hideSearchMode() {
        searchContainer.setVisibility(View.GONE);
        normalContainer.setVisibility(View.VISIBLE);
        EditText etSearch = requireView().findViewById(R.id.etSearch);
        etSearch.setText("");
    }

    /**
     * Filters the list of all events based on the provided query string and updates the search adapter.
     *
     * @param query The search query.
     */
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

    /**
     * Fetches all events from Firestore and updates the main event list.
     * Listens for real-time updates.
     */
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
                    String registrationStart = snapshot.getString("registration_start");
                    String registrationEnd = snapshot.getString("registration_end");
                    String eventDate = snapshot.getString("event_date");
                    if (id == null) id = snapshot.getId();
                    if (name == null) name = "";
                    if (description == null) description = "";
                    if (posterUrl == null) posterUrl = "";
                    if (registrationStart == null) registrationStart = "";
                    if (registrationEnd == null) registrationEnd = "";
                    if (eventDate == null) eventDate = "";

                    if (amount != 0) {
                        Event e = new Event(id, name, amount, registrationStart, registrationEnd, eventDate, description, posterUrl, sampleSize);
                        e.setHostId(snapshot.getString("createdBy"));
                        allEvents.add(e);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Inner adapter class for displaying events in a card-based RecyclerView within the Explore fragment.
     */
    private static class ExploreEventAdapter extends RecyclerView.Adapter<ExploreEventAdapter.ViewHolder> {
        private final List<Event> events;
        private final OnEventClickListener listener;

        /**
         * Interface for handling click events on event cards in the Explore view.
         */
        interface OnEventClickListener {
            /**
             * Called when an event card is clicked.
             * @param event The event object associated with the clicked card.
             */
            void onEventClick(Event event);
        }

        /**
         * Constructs an ExploreEventAdapter.
         * @param events The list of events to display.
         * @param listener The listener for click events.
         */
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

            holder.tvEventDate.setText(event.getFormattedEventDate());

            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        /**
         * ViewHolder for event card items in the Explore fragment.
         */
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate, tvEventTime;
            ImageView ivThumb;

            /**
             * Constructs a ViewHolder and binds UI components.
             * @param itemView The root view of the item layout.
             */
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
