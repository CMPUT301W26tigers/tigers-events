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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
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
 */
public class ExploreFragment extends Fragment {
    private static final String TAG = "ExploreFragment";
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> displayEvents = new ArrayList<>();
    private ExploreEventAdapter adapter;
    private RecyclerView rvEvents;
    private ChipGroup chipGroupFilters;
    private boolean filterAvailableOnly = false;
    private String filterDateFrom = null;
    private String filterDateTo = null;

    /**
     * Default constructor for ExploreFragment.
     */
    public ExploreFragment() { super(R.layout.fragment_explore); }

    /**
     * Called immediately after onCreateView has returned. Triggers expired-event cleanup,
     * wires up the RecyclerView with a live-filter adapter, attaches listeners for the
     * inbox button, filter sheet, and search bar, then begins a real-time load of all
     * public events from Firestore.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EventCleanupHelper.cleanupExpiredEvents();

        rvEvents = view.findViewById(R.id.rvEvents);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);

        adapter = new ExploreEventAdapter(displayEvents, event -> navigateToDetail(view, event));
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(adapter);

        view.findViewById(R.id.btnInboxNormal).setOnClickListener(this::openInbox);

        view.findViewById(R.id.ivMenu).setOnClickListener(v -> openFilterBottomSheet());

        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadAllEvents();
    }

    /**
     * Navigates to {@link EventDetailFragment} for the selected event, passing the event ID
     * and any available media URLs as navigation arguments.
     *
     * @param view  The current view, used to locate the NavController.
     * @param event The event the user tapped on.
     */
    private void navigateToDetail(View view, Event event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getId());
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            args.putString("posterUrl", event.getPosterUrl());
        }
        if (event.getHostProfilePictureUrl() != null && !event.getHostProfilePictureUrl().isEmpty()) {
            args.putString("hostProfilePictureUrl", event.getHostProfilePictureUrl());
        }
        Navigation.findNavController(view)
                .navigate(R.id.action_exploreFragment_to_eventDetailFragment, args);
    }

    /**
     * Navigates to the {@link InboxFragment}.
     *
     * @param view The view used to locate the NavController.
     */
    private void openInbox(View view) {
        Navigation.findNavController(view).navigate(R.id.inboxFragment);
    }

    /**
     * Shows the {@link ExploreFilterBottomSheet} pre-populated with the current filter state.
     * The callback updates the fragment's filter fields and re-runs {@link #applyFilters()}
     * as soon as the user confirms.
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
     * Combines the current search query with any active filter criteria (availability,
     * date range) and passes them to {@link ExploreFilterHelper} to produce a filtered
     * event list. Updates the adapter with the result and refreshes the chip strip.
     */
    private void applyFilters() {
        EditText etSearch = requireView().findViewById(R.id.etSearch);
        String query = etSearch.getText().toString();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(new Date());

        List<Event> newList = ExploreFilterHelper.applyAllFilters(
                allEvents, query, filterAvailableOnly, filterDateFrom, filterDateTo, today);
        adapter.updateEvents(newList);
        updateFilterChips();
    }

    /**
     * Rebuilds the active-filter chip row beneath the search bar. Each active filter
     * (availability toggle, date range) receives its own dismissible chip. Tapping the
     * close icon on a chip clears that filter and re-applies the remaining ones.
     * The chip group is hidden entirely when no filters are active.
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
            chip.setText(formatDateChipLabel(filterDateFrom, filterDateTo));
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

    /**
     * Builds a human-readable label for the active date-range chip, e.g.
     * "Apr 2 – Apr 10", "From Apr 2", or "Until Apr 10".
     *
     * @param from The start date in {@code yyyy-MM-dd} format, or {@code null}/{@code ""} if unset.
     * @param to   The end date in {@code yyyy-MM-dd} format, or {@code null}/{@code ""} if unset.
     * @return A non-null chip label string.
     */
    private String formatDateChipLabel(String from, String to) {
        boolean hasFrom = from != null && !from.isEmpty();
        boolean hasTo = to != null && !to.isEmpty();
        if (hasFrom && hasTo) return formatShortDate(from) + " – " + formatShortDate(to);
        else if (hasFrom) return "From " + formatShortDate(from);
        else return "Until " + formatShortDate(to);
    }

    /**
     * Converts a {@code yyyy-MM-dd} date string to a compact display form such as "Apr 2".
     * Returns the original string unchanged if parsing fails.
     *
     * @param dateStr The ISO date string to format.
     * @return The formatted short date string, or {@code dateStr} on error.
     */
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
     * Attaches a real-time Firestore snapshot listener to the "events" collection.
     * Private events are excluded. After each snapshot update, host display names and
     * profile picture URLs are resolved before the current filters are re-applied so
     * the list always reflects live data.
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
                    if (Boolean.TRUE.equals(isPrivate)) continue;

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
                    String location = snapshot.getString("location");
                    if (id == null) id = snapshot.getId();
                    if (name == null) name = "";
                    if (description == null) description = "";
                    if (posterUrl == null) posterUrl = "";
                    if (registrationStart == null) registrationStart = "";
                    if (registrationEnd == null) registrationEnd = "";
                    if (eventDate == null) eventDate = "";
                    if (location == null) location = "";

                    if (amount != 0) {
                        Event e = new Event(id, name, amount, registrationStart, registrationEnd, eventDate, description, posterUrl, sampleSize);
                        e.setHostId(snapshot.getString("createdBy"));
                        e.setLocation(location);
                        allEvents.add(e);
                    }
                }
                resolveHostNames(allEvents, this::applyFilters);
            }
        });
    }

    /**
     * Resolves display names and profile picture URLs for every unique host ID present in
     * the given event list. Each host document is fetched in parallel; once all requests
     * have returned (success or failure), the resolved values are written back to the
     * events and {@code onComplete} is invoked on the main thread.
     *
     * @param events     The list of events whose host metadata should be populated.
     * @param onComplete Callback executed after all host data has been resolved.
     */
    private void resolveHostNames(List<Event> events, Runnable onComplete) {
        Set<String> hostIds = new HashSet<>();
        for (Event e : events) {
            if (e.getHostId() != null && !e.getHostId().isEmpty()) hostIds.add(e.getHostId());
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
                            if (picUrl != null && !picUrl.isEmpty()) urlMap.put(hostId, picUrl);
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            for (Event e : events) {
                                e.setHostName(nameMap.getOrDefault(e.getHostId(), ""));
                                e.setHostProfilePictureUrl(urlMap.get(e.getHostId()));
                            }
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            for (Event ev : events) {
                                ev.setHostName(nameMap.getOrDefault(ev.getHostId(), ""));
                                ev.setHostProfilePictureUrl(urlMap.get(ev.getHostId()));
                            }
                            onComplete.run();
                        }
                    });
        }
    }

    /**
     * RecyclerView adapter for the Explore screen. Uses {@link DiffUtil} for efficient
     * incremental updates whenever the filtered event list changes, minimising flicker.
     */
    private static class ExploreEventAdapter extends RecyclerView.Adapter<ExploreEventAdapter.ViewHolder> {
        private final List<Event> events;
        private final OnEventClickListener listener;

        /** Callback interface for taps on an event card. */
        interface OnEventClickListener {
            /**
             * Called when the user taps an event card.
             *
             * @param event The event associated with the tapped card.
             */
            void onEventClick(Event event);
        }

        /**
         * Constructs the adapter.
         *
         * @param events   The initial (mutable) list of events to display.
         * @param listener Callback invoked when an event card is tapped.
         */
        ExploreEventAdapter(List<Event> events, OnEventClickListener listener) {
            this.events = events;
            this.listener = listener;
        }

        /**
         * Inflates the event card layout and wraps it in a {@link ViewHolder}.
         *
         * @param parent   The parent ViewGroup into which the new view will be inserted.
         * @param viewType Unused; all items share the same view type.
         * @return A new {@link ViewHolder} instance.
         */
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_explore_event_card, parent, false);
            return new ViewHolder(view);
        }

        /**
         * Binds event data to the card views at {@code position}. Handles the host avatar
         * (profile image or initial-letter fallback), poster visibility, description, and
         * location row.
         *
         * @param holder   The ViewHolder to populate.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Event event = events.get(position);
            holder.tvEventName.setText(event.getName());
            holder.tvEventHost.setText(event.getHostName() != null ? event.getHostName() : "");
            holder.tvEventDate.setText(event.getFormattedEventDate());

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

            String description = event.getDescription();
            if (description != null && !description.isEmpty()) {
                holder.tvDescription.setText(description);
                holder.tvDescription.setVisibility(View.VISIBLE);
            } else {
                holder.tvDescription.setVisibility(View.GONE);
            }

            String location = event.getLocation();
            if (location != null && !location.isEmpty()) {
                holder.tvLocation.setText(location);
                holder.llLocation.setVisibility(View.VISIBLE);
            } else {
                holder.llLocation.setVisibility(View.GONE);
            }

            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                holder.ivPoster.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(event.getPosterUrl()).into(holder.ivPoster);
            } else {
                holder.ivPoster.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        /**
         * Diffs the new list against the current one using {@link DiffUtil}, updates the
         * backing list in-place, and dispatches the minimal set of change notifications
         * to the RecyclerView to avoid a full rebind.
         *
         * @param newList The updated list of events to display.
         */
        void updateEvents(List<Event> newList) {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return events.size(); }
                @Override public int getNewListSize() { return newList.size(); }
                @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                    return events.get(oldPos).getId().equals(newList.get(newPos).getId());
                }
                @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                    Event o = events.get(oldPos), n = newList.get(newPos);
                    return o.getId().equals(n.getId())
                            && safeEqual(o.getName(), n.getName())
                            && safeEqual(o.getPosterUrl(), n.getPosterUrl())
                            && safeEqual(o.getHostName(), n.getHostName())
                            && safeEqual(o.getHostProfilePictureUrl(), n.getHostProfilePictureUrl());
                }
                private boolean safeEqual(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            });
            events.clear();
            events.addAll(newList);
            result.dispatchUpdatesTo(this);
        }

        /**
         * Returns the total number of events currently displayed.
         *
         * @return The size of the event list.
         */
        @Override
        public int getItemCount() { return events.size(); }

        /** Holds references to all views in a single explore-event card. */
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate, tvDescription, tvLocation;
            ImageView ivPoster, ivAvatarImage;
            View llLocation;

            /**
             * Binds view references from the inflated explore-event card layout.
             *
             * @param itemView The root view of the card layout.
             */
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEventName = itemView.findViewById(R.id.tvExploreEventName);
                tvEventHost = itemView.findViewById(R.id.tvExploreHostName);
                tvAvatarLetter = itemView.findViewById(R.id.tvExploreAvatarLetter);
                tvEventDate = itemView.findViewById(R.id.tvExploreDate);
                tvDescription = itemView.findViewById(R.id.tvExploreDescription);
                tvLocation = itemView.findViewById(R.id.tvExploreLocation);
                llLocation = itemView.findViewById(R.id.llExploreLocation);
                ivPoster = itemView.findViewById(R.id.ivExplorePoster);
                ivAvatarImage = itemView.findViewById(R.id.ivExploreAvatarImage);
            }
        }
    }
}
