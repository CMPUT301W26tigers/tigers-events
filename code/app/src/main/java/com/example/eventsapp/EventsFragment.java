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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fragment that displays a list of events associated with the current user.
 * It provides a toggle to switch between events created by the user and events
 * the user is registered for or waitlisted on.
 */
public class EventsFragment extends Fragment {
    private static final String TAG = "EventsFragment";

    private final List<Event> eventList = new ArrayList<>();
    private EventCardAdapter adapter;
    private RecyclerView rvMyEvents;
    private LinearLayout emptyStateContainer;
    private MaterialButtonToggleGroup toggleEventType;
    private MaterialButton btnCreateEvent;
    private ChipGroup chipGroupFilter;
    private int loadGeneration = 0; // To counter stupid bug where events are shown twice

    /**
     * Default constructor for EventsFragment.
     * Uses the layout R.layout.fragment_events.
     */
    public EventsFragment() { super(R.layout.fragment_events); }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned. Initializes the UI components, sets up the RecyclerView with a custom adapter,
     * and triggers the initial event loading.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMyEvents = view.findViewById(R.id.rvMyEvents);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        toggleEventType = view.findViewById(R.id.toggleEventType);
        btnCreateEvent = view.findViewById(R.id.btnCreateEvent);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        btnCreateEvent.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_eventsFragment_to_createEventFragment));

        adapter = new EventCardAdapter(eventList, event -> {
            Bundle args = new Bundle();
            args.putString("eventId", event.getId());

            boolean isCreatedTab = toggleEventType.getCheckedButtonId() == R.id.btnCreated;

            if (EditEventFragment.shouldNavigateToEdit(isCreatedTab, event.isFromHistory())) {
                // Active event the user created -> go to edit page
                Navigation.findNavController(view)
                        .navigate(R.id.action_eventsFragment_to_editEventFragment, args);
            } else {
                // Registered event or history event -> go to detail page
                if (event.isFromHistory()) {
                    args.putBoolean("fromHistory", true);
                }
                Navigation.findNavController(view)
                        .navigate(R.id.action_eventsFragment_to_eventDetailFragment, args);
            }
        });
        rvMyEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyEvents.setAdapter(adapter);

        // Clean up expired events on load
        EventCleanupHelper.cleanupExpiredEvents();

        loadEvents();
        updateCreateButtonVisibility();

        toggleEventType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateCreateButtonVisibility();
                loadEvents();
            }
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            loadEvents();
        });
    }

    private void updateCreateButtonVisibility() {
        boolean isCreatedTab = toggleEventType.getCheckedButtonId() == R.id.btnCreated;
        btnCreateEvent.setVisibility(isCreatedTab ? View.VISIBLE : View.GONE);
    }

    /**
     * Determines which events to load based on the current toggle state (Created vs. Registered).
     */
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
        loadGeneration++;
        final int currentGeneration = loadGeneration;

        if (checkedId == R.id.btnCreated) {
            loadCreatedEvents(userId, currentGeneration);
        } else {
            loadRegisteredEvents(userId, currentGeneration);
        }
    }

    /**
     * Fetches events from Firestore where the `createdBy` field matches the current user's ID.
     *
     * @param userId The unique ID of the current user.
     */
    private void loadCreatedEvents(String userId, int generation) {
        FirebaseFirestore.getInstance().collection("events")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || generation != loadGeneration) return;
                    eventList.clear();
                    Set<String> activeEventIds = new HashSet<>();
                    for (QueryDocumentSnapshot snapshot : querySnapshot) {
                        Event event = parseEvent(snapshot);
                        if (event != null) {
                            eventList.add(event);
                            activeEventIds.add(event.getId());
                        }
                    }
                    // Also load expired history events for the organizer
                    loadHistoryEvents(userId, "ORGANIZED", activeEventIds, generation);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load created events", e);
                });
    }

    /**
     * Fetches events from Firestore where the current user is listed in the `entrants` subcollection.
     * This method iterates through all events and checks for the user's presence in each subcollection.
     *
     * @param userId The unique ID of the current user.
     */
    private void loadRegisteredEvents(String userId, int generation) {
        FirebaseFirestore.getInstance().collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || generation != loadGeneration) return;
                    eventList.clear();

                    if (querySnapshot.isEmpty()) {
                        // No active events, but still load history
                        loadHistoryEvents(userId, null, new HashSet<>(), generation);
                        return;
                    }

                    List<QueryDocumentSnapshot> allDocs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        allDocs.add(doc);
                    }

                    Set<String> activeEventIds = new HashSet<>();
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
                                    if (!isAdded() || generation != loadGeneration) return;
                                    String entrantStatus = null;
                                    for (QueryDocumentSnapshot entrantDoc : entrantSnapshot) {
                                        entrantStatus = entrantDoc.getString("status");
                                        if (entrantStatus != null) {
                                            break;
                                        }
                                    }

                                    if (entrantStatus != null) {
                                        Event event = parseEvent(snapshot);
                                        if (event != null) {
                                            event.setEntrantStatus(entrantStatus);
                                            eventList.add(event);
                                            activeEventIds.add(event.getId());
                                        }
                                    }
                                    if (remaining.decrementAndGet() == 0) {
                                        // After loading active events, merge in expired history
                                        loadHistoryEvents(userId, null, activeEventIds, generation);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    Log.e(TAG, "Failed to check entrants", e);
                                    if (remaining.decrementAndGet() == 0) {
                                        loadHistoryEvents(userId, null, activeEventIds, generation);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load events", e);
                });
    }

    /**
     * Parses a Firestore {@link QueryDocumentSnapshot} into an {@link Event} object.
     *
     * @param snapshot The document snapshot from Firestore.
     * @return A new Event object, or null if the data is invalid (e.g., amount is 0).
     */
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

        if (registrationStart == null) registrationStart = "";
        if (registrationEnd == null) registrationEnd = "";
        if (eventDate == null) eventDate = "";

        if (id == null) id = snapshot.getId();
        if (name == null) name = "";
        if (description == null) description = "";
        if (posterUrl == null) posterUrl = "";

        if (amount != 0) {
            Event e = new Event(id, name, amount, registrationStart, registrationEnd, eventDate, description, posterUrl, sampleSize);
            e.setHostId(snapshot.getString("createdBy"));
            return e;
        }
        return null;
    }

    /**
     * Updates the visibility of the RecyclerView and the empty state message based on the event list size.
     */
    private void updateEmptyState() {
        if (eventList.isEmpty()) {
            rvMyEvents.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            rvMyEvents.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Loads expired event history records from the user's personal eventHistory collection
     * and merges them into the event list, skipping any that are already present as active events.
     * Used in the Registered -> past tab in My Events
     *
     * @param userId The current users ID.
     * @param statusFilter If non-null, only load history events with this entrantStatus (ex. organized)
     *                     If null, load all history events except "ORGANIZED".
     * @param activeEventIds Set of event IDs already loaded from active events (to avoid duplicates).
     */
    private void loadHistoryEvents(String userId, @Nullable String statusFilter, Set<String> activeEventIds, int generation) {
        FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("eventHistory")
                .whereEqualTo("expired", true)
                .get()
                .addOnSuccessListener(historySnapshot -> {
                    if (!isAdded() || generation != loadGeneration) return;
                    for (DocumentSnapshot doc : historySnapshot.getDocuments()) {
                        String eventId = doc.getString("id");
                        if (eventId == null || activeEventIds.contains(eventId)) continue;

                        String historyStatus = doc.getString("entrantStatus");
                        // Filter: "ORGANIZED" tab only shows ORGANIZED, "Registered" tab shows everything except ORGANIZED
                        if (statusFilter != null) {
                            if (!statusFilter.equals(historyStatus)) continue;
                        } else {
                            if ("ORGANIZED".equals(historyStatus)) continue;
                        }

                        Event event = parseHistoryEvent(doc);
                        if (event != null) {
                            event.setEntrantStatus(historyStatus);
                            event.setFromHistory(true);
                            eventList.add(event);
                        }
                    }
                    filterEventsByDate();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load history events", e);
                    // Still display whatever active events we have
                    filterEventsByDate();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    /**
     * Parses a history document into an Event object.
     */
    private Event parseHistoryEvent(DocumentSnapshot doc) {
        String id = doc.getString("id");
        String name = doc.getString("name");
        Long amountLong = doc.getLong("amount");
        int amount = (amountLong != null) ? amountLong.intValue() : 0;
        String description = doc.getString("description");
        String posterUrl = doc.getString("posterUrl");
        Long sampleLong = doc.getLong("sampleSize");
        int sampleSize = (sampleLong != null) ? sampleLong.intValue() : 0;

        String registrationStart = doc.getString("registration_start");
        String registrationEnd = doc.getString("registration_end");
        String eventDate = doc.getString("event_date");

        if (registrationStart == null) registrationStart = "";
        if (registrationEnd == null) registrationEnd = "";
        if (eventDate == null) eventDate = "";
        if (id == null) id = doc.getId();
        if (name == null) name = "";
        if (description == null) description = "";
        if (posterUrl == null) posterUrl = "";

        if (amount != 0) {
            Event e = new Event(id, name, amount, registrationStart, registrationEnd, eventDate, description, posterUrl, sampleSize);
            e.setHostId(doc.getString("createdBy"));
            return e;
        }
        return null;
    }

    /**
     * Returns the currently selected chip filter value.
     * @return "upcoming", "past", or "all".
     */
    private String getSelectedChipFilter() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        if (checkedId == R.id.chipUpcoming) return "upcoming";
        if (checkedId == R.id.chipPast) return "past";
        return "all";
    }

    /**
     * Filters the event list based on the selected chip filter (Upcoming/Past/All).
     * Compares event_date strings (yyyy-MM-dd format) against today's date.
     * Events with terminal statuses (DECLINED, CANCELLED) are treated as "past"
     * regardless of the event date, since the user is done with them.
     */
    private void filterEventsByDate() {
        String filter = getSelectedChipFilter();
        if ("all".equals(filter)) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(new Date());

        eventList.removeIf(event -> {
            String eventDate = event.getEvent_date();
            if (eventDate == null || eventDate.isEmpty()) return false;

            String status = event.getEntrantStatus();
            boolean isTerminalStatus = "DECLINED".equals(status) || "CANCELLED".equals(status);

            if ("upcoming".equals(filter)) {
                // Upcoming: remove past events and terminal-status events
                return eventDate.compareTo(today) < 0 || isTerminalStatus;
            } else {
                // Past: keep past events and terminal-status events
                return eventDate.compareTo(today) >= 0 && !isTerminalStatus;
            }
        });
    }

    /**
     * Inner adapter class for displaying events in a card-based RecyclerView.
     */
    private static class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.ViewHolder> {
        private final List<Event> events;
        private final OnEventClickListener listener;

        /**
         * Interface for handling click events on individual event cards.
         */
        interface OnEventClickListener {
            /**
             * Called when an event card is clicked.
             * @param event The event object associated with the clicked card.
             */
            void onEventClick(Event event);
        }

        /**
         * Constructs an EventCardAdapter.
         * @param events The list of events to display.
         * @param listener The listener for click events.
         */
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

            String status = event.getEntrantStatus();
            if (status != null && !status.isEmpty()) {
                holder.tvEntrantStatus.setVisibility(View.VISIBLE);
                holder.tvEntrantStatus.setText(formatStatus(status));
                holder.tvEntrantStatus.setTextColor(getStatusColor(status));
            } else {
                holder.tvEntrantStatus.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        /**
         * ViewHolder for event card items.
         */
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventHost, tvAvatarLetter, tvEventDate, tvEventTime, tvEntrantStatus;
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
                tvEntrantStatus = itemView.findViewById(R.id.tvEntrantStatus);
            }
        }

        /**
         * Formats the entrant status for display.
         * @param status The raw status string from Firestore.
         * @return A user-friendly status label.
         */
        private static String formatStatus(String status) {
            switch (status) {
                case "APPLIED": return "Waitlisted";
                case "INVITED": return "Invited";
                case "ACCEPTED": return "Accepted";
                case "DECLINED": return "Declined";
                case "CANCELLED": return "Cancelled";
                case "ORGANIZED": return "Organized";
                default: return status;
            }
        }

        /**
         * Returns a color for the given entrant status because im extra
         * @param status The raw status string.
         * @return An ARGB color integer.
         */
        private static int getStatusColor(String status) {
            switch (status) {
                case "ACCEPTED": return 0xFF4CAF50;  // green
                case "INVITED": return 0xFF2196F3;   // blue
                case "ORGANIZED": return 0xFF9C27B0; // purple
                case "DECLINED":
                case "CANCELLED": return 0xFFFF5722; // red-orange
                default: return 0xFF9E9E9E;          // gray
            }
        }
    }
}
