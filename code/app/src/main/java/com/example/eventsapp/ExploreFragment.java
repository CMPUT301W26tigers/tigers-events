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

    private void openInbox(View view) {
        Navigation.findNavController(view).navigate(R.id.inboxFragment);
    }

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

    private void applyFilters() {
        EditText etSearch = requireView().findViewById(R.id.etSearch);
        String query = etSearch.getText().toString();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(new Date());

        List<Event> newList = ExploreFilterHelper.applyAllFilters(
                allEvents, query, filterAvailableOnly, filterDateFrom, filterDateTo, today);
        adapter.updateEvents(newList);
        updateFilterChips();
    }

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

    private String formatDateChipLabel(String from, String to) {
        boolean hasFrom = from != null && !from.isEmpty();
        boolean hasTo = to != null && !to.isEmpty();
        if (hasFrom && hasTo) return formatShortDate(from) + " – " + formatShortDate(to);
        else if (hasFrom) return "From " + formatShortDate(from);
        else return "Until " + formatShortDate(to);
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
                    .inflate(R.layout.item_explore_event_card, parent, false);
            return new ViewHolder(view);
        }

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

        @Override
        public int getItemCount() { return events.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate, tvDescription, tvLocation;
            ImageView ivPoster, ivAvatarImage;
            View llLocation;

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
