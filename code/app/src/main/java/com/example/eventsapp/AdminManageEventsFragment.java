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
import java.util.List;

public class AdminManageEventsFragment extends Fragment {

    private static final String TAG = "AdminManageEvents";

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> filteredEvents = new ArrayList<>();
    private final List<Event> filteredPosterEvents = new ArrayList<>();

    private EventCardAdapter eventAdapter;
    private PosterGridAdapter posterAdapter;
    private RecyclerView rvAdminEvents;
    private RecyclerView rvAdminPosters;

    public AdminManageEventsFragment() {
        super(R.layout.fragment_admin_manage_events);
    }

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

                    EditText etSearch = getView() != null ? getView().findViewById(R.id.etSearchAdmin) : null;
                    String query = (etSearch != null && etSearch.getText() != null)
                            ? etSearch.getText().toString() : "";
                    applyFilter(query);
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
            holder.tvEventDate.setText(event.getFormattedEventDate());

            // Hide entrant status in admin view
            holder.tvEntrantStatus.setVisibility(View.GONE);

            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(event.getPosterUrl()).into(holder.ivThumb);
                holder.avatarCircle.setVisibility(View.GONE);
                holder.cardThumb.setVisibility(View.VISIBLE);
            } else {
                holder.avatarCircle.setVisibility(View.VISIBLE);
                holder.cardThumb.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate, tvEntrantStatus;
            ImageView ivThumb;
            View avatarCircle, cardThumb;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEventName = itemView.findViewById(R.id.tvEventName);
                tvEventHost = itemView.findViewById(R.id.tvEventHost);
                tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
                tvEventDate = itemView.findViewById(R.id.tvEventDate);
                tvEntrantStatus = itemView.findViewById(R.id.tvEntrantStatus);
                ivThumb = itemView.findViewById(R.id.ivThumb);
                avatarCircle = itemView.findViewById(R.id.avatarCircle);
                cardThumb = itemView.findViewById(R.id.cardThumb);
            }
        }
    }

    // ── Poster Grid Adapter ──

    private static class PosterGridAdapter extends RecyclerView.Adapter<PosterGridAdapter.ViewHolder> {
        private final List<Event> events;
        private final EventCardAdapter.OnEventClickListener listener;

        PosterGridAdapter(List<Event> events, EventCardAdapter.OnEventClickListener listener) {
            this.events = events;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_poster, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Event event = events.get(position);
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterUrl())
                    .into(holder.ivPosterGrid);
            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPosterGrid;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPosterGrid = itemView.findViewById(R.id.ivPosterGrid);
            }
        }
    }
}
