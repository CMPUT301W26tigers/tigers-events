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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin fragment for browsing and managing all events in the platform.
 *
 * <p>Provides two view modes toggled via a {@link com.google.android.material.button.MaterialButtonToggleGroup}:
 * <ul>
 *   <li><b>Event list</b> – a card-based {@link RecyclerView} showing event name, host
 *       avatar/initial, date, and an optional thumbnail.</li>
 *   <li><b>Poster grid</b> – a 2-column grid showing only events that have a poster image.</li>
 * </ul>
 *
 * <p>Host names and avatars are resolved asynchronously from the {@code users} collection
 * after the initial event load.  A search bar filters both views simultaneously.
 */
public class AdminManageEventsFragment extends Fragment {

    private static final String TAG = "AdminManageEvents";

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private final List<Event> filteredPosterEvents = new ArrayList<>();

    private EventCardAdapter eventAdapter;
    private PosterGridAdapter posterAdapter;
    private RecyclerView rvAdminEvents;
    private RecyclerView rvAdminPosters;

    /**
     * Required public no-arg constructor. Supplies the layout resource so the
     * framework can recreate this fragment automatically.
     */
    public AdminManageEventsFragment() {
        super(R.layout.fragment_admin_manage_events);
    }

    /**
     * Sets up both RecyclerViews, the view-mode toggle, the search bar, and triggers
     * the initial Firestore event load.
     *
     * @param view               the inflated layout root
     * @param savedInstanceState previously saved state, or {@code null}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBackToUser).setOnClickListener(v ->
                requireActivity().finish());

        rvAdminEvents = view.findViewById(R.id.rvAdminEvents);
        rvAdminPosters = view.findViewById(R.id.rvAdminPosters);

        // Event card list
        eventAdapter = new EventCardAdapter(filteredEvents, this::navigateToEventDetail);
        rvAdminEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdminEvents.setAdapter(eventAdapter);

        // Poster grid
        posterAdapter = new PosterGridAdapter(filteredPosterEvents, this::navigateToEventDetail);
        rvAdminPosters.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvAdminPosters.setAdapter(posterAdapter);

        // Toggle between Event View and Poster View
        MaterialButtonToggleGroup toggleViewMode = view.findViewById(R.id.toggleViewMode);
        toggleViewMode.check(R.id.btnEventView);
        toggleViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnEventView) {
                    rvAdminEvents.setVisibility(View.VISIBLE);
                    rvAdminPosters.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnPosterView) {
                    rvAdminEvents.setVisibility(View.GONE);
                    rvAdminPosters.setVisibility(View.VISIBLE);
                }
            }
        });

        // Search
        EditText etSearch = view.findViewById(R.id.etSearchAdmin);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s.toString());
            }
        });

        loadAllEvents();
    }

    private void navigateToEventDetail(Event event) {
        Bundle args = new Bundle();
        args.putSerializable("event", event);
        Navigation.findNavController(requireView())
                .navigate(R.id.adminEventDetailFragment, args);
    }

    private void applyFilter(String query) {
        filteredEvents.clear();
        filteredPosterEvents.clear();

        String lowerQuery = query.trim().toLowerCase();

        for (Event event : allEvents) {
            boolean matches = lowerQuery.isEmpty()
                    || event.getName().toLowerCase().contains(lowerQuery)
                    || event.getDescription().toLowerCase().contains(lowerQuery);
            if (matches) {
                filteredEvents.add(event);
                if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                    filteredPosterEvents.add(event);
                }
            }
        }

        eventAdapter.notifyDataSetChanged();
        posterAdapter.notifyDataSetChanged();
    }

    private void loadAllEvents() {
        FirebaseFirestore.getInstance().collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading events", error);
                        return;
                    }
                    if (value == null || !isAdded()) return;

                    allEvents.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        Event event = parseEvent(snapshot);
                        if (event != null) {
                            allEvents.add(event);
                        }
                    }

                    resolveHostNames(allEvents, () -> {
                        EditText etSearch = getView() != null ? getView().findViewById(R.id.etSearchAdmin) : null;
                        String query = (etSearch != null && etSearch.getText() != null)
                                ? etSearch.getText().toString() : "";
                        applyFilter(query);
                    });
                });
    }

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

    private Event parseEvent(QueryDocumentSnapshot snapshot) {
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
            Event e = new Event(id, name, amount, registrationStart, registrationEnd,
                    eventDate, description, posterUrl, sampleSize);
            e.setHostId(snapshot.getString("createdBy"));
            return e;
        }
        return null;
    }

    // ── Event Card Adapter ──

    /**
     * Adapter that renders a list of {@link Event} objects as card rows.
     * Shows the event name, host avatar (photo or initial letter), event date, and
     * an optional thumbnail; hides the entrant-status chip used in the entrant-facing view.
     */
    private static class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.ViewHolder> {
        private final List<Event> events;
        private final OnEventClickListener listener;

        /**
         * Callback invoked when an event card is tapped.
         */
        interface OnEventClickListener {
            /**
             * @param event the event whose card was tapped
             */
            void onEventClick(Event event);
        }

        /**
         * @param events   the live list of events to display
         * @param listener click callback for navigation to the detail screen
         */
        EventCardAdapter(List<Event> events, OnEventClickListener listener) {
            this.events = events;
            this.listener = listener;
        }

        /**
         * Inflates {@code item_event_card} and wraps it in a {@link ViewHolder}.
         *
         * @param parent   the parent RecyclerView
         * @param viewType unused (single view type)
         * @return a new view holder
         */
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event_card, parent, false);
            return new ViewHolder(view);
        }

        /**
         * Binds the event at {@code position} to {@code holder}.  Loads the host
         * avatar with Glide if a URL is available, otherwise shows the host's initial.
         * Hides the entrant-status label.
         *
         * @param holder   the view holder to populate
         * @param position adapter position
         */
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

            // Hide entrant status in admin view
            holder.tvEntrantStatus.setVisibility(View.GONE);

            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(event.getPosterUrl()).into(holder.ivThumb);
                holder.ivThumb.setVisibility(View.VISIBLE);
            } else {
                holder.ivThumb.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        /**
         * @return the number of events currently in the filtered list
         */
        @Override
        public int getItemCount() {
            return events.size();
        }

        /** Holds view references for one event card row. */
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate, tvEntrantStatus;
            ImageView ivThumb, ivAvatarImage;

            /**
             * @param itemView the inflated {@code item_event_card} view
             */
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEventName = itemView.findViewById(R.id.tvEventName);
                tvEventHost = itemView.findViewById(R.id.tvEventHost);
                tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
                tvEventDate = itemView.findViewById(R.id.tvEventDate);
                tvEntrantStatus = itemView.findViewById(R.id.tvEntrantStatus);
                ivThumb = itemView.findViewById(R.id.ivThumb);
                ivAvatarImage = itemView.findViewById(R.id.ivAvatarImage);
            }
        }
    }

    // ── Poster Grid Adapter ──

    /**
     * Adapter that displays events in a 2-column grid using their poster images.
     * Only events with a non-empty {@code posterUrl} are included in this adapter's list.
     */
    private static class PosterGridAdapter extends RecyclerView.Adapter<PosterGridAdapter.ViewHolder> {
        private final List<Event> events;
        private final EventCardAdapter.OnEventClickListener listener;

        /**
         * @param events   the live list of poster-bearing events
         * @param listener click callback shared with the card adapter for navigation
         */
        PosterGridAdapter(List<Event> events, EventCardAdapter.OnEventClickListener listener) {
            this.events = events;
            this.listener = listener;
        }

        /**
         * Inflates {@code item_admin_poster} and wraps it in a {@link ViewHolder}.
         *
         * @param parent   the parent RecyclerView
         * @param viewType unused (single view type)
         * @return a new view holder
         */
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_poster, parent, false);
            return new ViewHolder(view);
        }

        /**
         * Loads the event poster into {@code holder.ivPosterGrid} via Glide and
         * attaches the navigation click listener.
         *
         * @param holder   view holder to populate
         * @param position adapter position
         */
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Event event = events.get(position);
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterUrl())
                    .into(holder.ivPosterGrid);
            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        /**
         * @return the number of poster-bearing events currently in the filtered list
         */
        @Override
        public int getItemCount() {
            return events.size();
        }

        /** Holds the poster {@link ImageView} for one grid cell. */
        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPosterGrid;

            /**
             * @param itemView the inflated {@code item_admin_poster} view
             */
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPosterGrid = itemView.findViewById(R.id.ivPosterGrid);
            }
        }
    }
}
