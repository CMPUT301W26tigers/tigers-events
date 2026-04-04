package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private ChipGroup chipGroupFilters;
    private boolean filterAvailableOnly = false;
    private String filterDateFrom = null;
    private String filterDateTo = null;

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
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);

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

        ImageButton btnInboxNormal = view.findViewById(R.id.btnInboxNormal);
        ImageButton btnInboxSearch = view.findViewById(R.id.btnTopIconSearch);
        btnInboxNormal.setOnClickListener(this::openInbox);
        btnInboxSearch.setOnClickListener(this::openInbox);

        // Back button exits search mode
        view.findViewById(R.id.btnBack).setOnClickListener(v -> hideSearchMode());

        // Filter button opens the filter bottom sheet
        view.findViewById(R.id.ivMenu).setOnClickListener(v -> openFilterBottomSheet());

        // Search text filtering
        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
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

    private void openInbox(View view) {
        Navigation.findNavController(view).navigate(R.id.inboxFragment);
    }

    /**
     * Switches the UI to search mode, showing the search container and hiding the normal list.
     */
    private void showSearchMode() {
        normalContainer.setVisibility(View.GONE);
        searchContainer.setVisibility(View.VISIBLE);
        applyFilters();
    }

    /**
     * Exits search mode, showing the normal list and clearing the search input.
     * Resets all filters.
     */
    private void hideSearchMode() {
        searchContainer.setVisibility(View.GONE);
        normalContainer.setVisibility(View.VISIBLE);
        filterAvailableOnly = false;
        filterDateFrom = null;
        filterDateTo = null;
        chipGroupFilters.removeAllViews();
        chipGroupFilters.setVisibility(View.GONE);
        EditText etSearch = requireView().findViewById(R.id.etSearch);
        etSearch.setText("");
    }

    /**
     * Opens the filter bottom sheet with the current filter state.
     */
    private void openFilterBottomSheet() {
        ExploreFilterBottomSheet sheet = ExploreFilterBottomSheet.newInstance(
                filterAvailableOnly, filterDateFrom, filterDateTo);
        sheet.setOnFilterAppliedListener((availableOnly, dateFrom, dateTo) -> {
            filterAvailableOnly = availableOnly;
            filterDateFrom = dateFrom;
            filterDateTo = dateTo;
            applyFilters();
        });
        sheet.show(getChildFragmentManager(), "filter_sheet");
    }

    /**
     * Applies all active filters (text search, available only, date range) and updates the search results.
     */
    private void applyFilters() {
        EditText etSearch = requireView().findViewById(R.id.etSearch);
        String query = etSearch.getText().toString();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(new Date());

        filteredEvents.clear();
        filteredEvents.addAll(ExploreFilterHelper.applyAllFilters(
                allEvents, query, filterAvailableOnly, filterDateFrom, filterDateTo, today));
        searchAdapter.notifyDataSetChanged();
        updateFilterChips();
    }

    /**
     * Updates the filter chip indicators below the search bar.
     */
    private void updateFilterChips() {
        chipGroupFilters.removeAllViews();

        if (filterAvailableOnly) {
            Chip chip = new Chip(requireContext());
            chip.setText("Available Only");
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                filterAvailableOnly = false;
                applyFilters();
            });
            chipGroupFilters.addView(chip);
        }

        boolean hasFrom = filterDateFrom != null && !filterDateFrom.isEmpty();
        boolean hasTo = filterDateTo != null && !filterDateTo.isEmpty();
        if (hasFrom || hasTo) {
            Chip chip = new Chip(requireContext());
            String label = formatDateChipLabel(filterDateFrom, filterDateTo);
            chip.setText(label);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                filterDateFrom = null;
                filterDateTo = null;
                applyFilters();
            });
            chipGroupFilters.addView(chip);
        }

        chipGroupFilters.setVisibility(chipGroupFilters.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private String formatDateChipLabel(String from, String to) {
        boolean hasFrom = from != null && !from.isEmpty();
        boolean hasTo = to != null && !to.isEmpty();
        if (hasFrom && hasTo) {
            return formatShortDate(from) + " – " + formatShortDate(to);
        } else if (hasFrom) {
            return "From " + formatShortDate(from);
        } else {
            return "Until " + formatShortDate(to);
        }
    }

    private String formatShortDate(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
            SimpleDateFormat output = new SimpleDateFormat("MMM d", Locale.CANADA);
            Date date = input.parse(dateStr);
            return date != null ? output.format(date) : dateStr;
        } catch (Exception e) {
            return dateStr;
        }
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
                    Boolean isPrivate = snapshot.getBoolean("isPrivate");
                    if (Boolean.TRUE.equals(isPrivate)) {
                        continue;
                    }

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
                resolveHostNames(allEvents, () -> {
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }

    /**
     * Resolves display names for all unique host IDs in the given event list.
     * Once all names are resolved, sets hostName on each event and runs the callback.
     */
    private void resolveHostNames(List<Event> events, Runnable onComplete) {
        Set<String> hostIds = new HashSet<>();
        for (Event e : events) {
            if (e.getHostId() != null && !e.getHostId().isEmpty()) {
                hostIds.add(e.getHostId());
            }
        }
        if (hostIds.isEmpty()) {
            onComplete.run();
            return;
        }

        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> urlMap = new HashMap<>();
        final int[] remaining = {hostIds.size()};

        for (String hostId : hostIds) {
            FirebaseFirestore.getInstance().collection("users").document(hostId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String first = doc.getString("firstName");
                            String last = doc.getString("lastName");
                            if (first == null || first.isEmpty()) {
                                String fullName = doc.getString("name");
                                if (fullName != null && !fullName.isEmpty()) {
                                    String[] parts = fullName.trim().split("\\s+");
                                    first = parts[0];
                                    last = parts.length > 1 ? parts[parts.length - 1] : null;
                                }
                            }
                            if (first != null && !first.isEmpty()) {
                                String display = last != null && !last.isEmpty()
                                        ? first + " " + last.charAt(0) + "."
                                        : first;
                                nameMap.put(hostId, display);
                            }
                            String picUrl = doc.getString("profilePictureUrl");
                            if (picUrl != null && !picUrl.isEmpty()) {
                                urlMap.put(hostId, picUrl);
                            }
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            for (Event e : events) {
                                String resolved = nameMap.get(e.getHostId());
                                e.setHostName(resolved != null ? resolved : "");
                                e.setHostProfilePictureUrl(urlMap.get(e.getHostId()));
                            }
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            for (Event ev : events) {
                                String resolved = nameMap.get(ev.getHostId());
                                ev.setHostName(resolved != null ? resolved : "");
                                ev.setHostProfilePictureUrl(urlMap.get(ev.getHostId()));
                            }
                            onComplete.run();
                        }
                    });
        }
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
            holder.tvEventHost.setText(event.getHostName() != null ? event.getHostName() : "");

            String hostPicUrl = event.getHostProfilePictureUrl();
            if (hostPicUrl != null && !hostPicUrl.isEmpty()) {
                holder.ivAvatarImage.setVisibility(View.VISIBLE);
                holder.tvAvatarLetter.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext()).load(hostPicUrl).circleCrop().into(holder.ivAvatarImage);
            } else {
                holder.ivAvatarImage.setVisibility(View.GONE);
                holder.tvAvatarLetter.setVisibility(View.VISIBLE);
                String hostName = event.getHostName();
                holder.tvAvatarLetter.setText(hostName != null && !hostName.isEmpty()
                        ? String.valueOf(hostName.charAt(0)).toUpperCase() : "?");
            }

            holder.tvEventDate.setText(event.getFormattedEventDate());

            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(event.getPosterUrl()).into(holder.ivThumb);
                holder.ivThumb.setVisibility(View.VISIBLE);
            } else {
                holder.ivThumb.setVisibility(View.GONE);
            }

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
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate;
            ImageView ivThumb, ivAvatarImage;

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
                ivThumb = itemView.findViewById(R.id.ivThumb);
                ivAvatarImage = itemView.findViewById(R.id.ivAvatarImage);
            }
        }
    }
}
