package com.example.eventsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment that displays the user's notification inbox.
 * It shows a list of {@link UserNotification} objects and allows users to:
 * - View details of each notification.
 * - Accept or decline event invitations directly from the inbox.
 */
public class InboxFragment extends Fragment {
    private final InvitationResponseController invitationResponseController = new InvitationResponseController();
    private final List<UserNotification> notifications = new ArrayList<>();
    private LinearLayout notificationContainer;
    private TextView emptyStateView;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    /**
     * Default constructor for InboxFragment.
     * Uses the layout R.layout.fragment_inbox.
     */
    public InboxFragment() {
        super(R.layout.fragment_inbox);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned. Initializes the UI components and triggers the rendering of notifications.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        ImageButton backButton = view.findViewById(R.id.btnInboxBack);
        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        notificationContainer = view.findViewById(R.id.notificationContainer);
        emptyStateView = view.findViewById(R.id.tvEmptyState);
        loadNotifications();
    }

    /**
     * Renders the list of notifications in the notification container.
     * Clears existing views and inflates a new view for each notification.
     */
    private void renderNotifications(List<UserNotification> notificationItems) {
        notificationContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<UserNotification> waitlistedNotifications = new ArrayList<>();
        int firstWaitlistedIndex = -1;

        for (int i = 0; i < notificationItems.size(); i++) {
            UserNotification notification = notificationItems.get(i);
            if (notification.getType() == UserNotification.Type.WAITLISTED) {
                waitlistedNotifications.add(notification);
                if (firstWaitlistedIndex == -1) {
                    firstWaitlistedIndex = i;
                }
            }
        }

        boolean renderedWaitlistGroup = false;
        for (int i = 0; i < notificationItems.size(); i++) {
            UserNotification notification = notificationItems.get(i);
            if (notification.getType() == UserNotification.Type.WAITLISTED) {
                if (!renderedWaitlistGroup && i == firstWaitlistedIndex) {
                    View waitlistGroupView = inflater.inflate(
                            R.layout.item_inbox_waitlist_group,
                            notificationContainer,
                            false
                    );
                    bindWaitlistGroup(waitlistGroupView, waitlistedNotifications);
                    notificationContainer.addView(waitlistGroupView);
                    renderedWaitlistGroup = true;
                }
                continue;
            }

            View itemView = inflater.inflate(R.layout.item_inbox_notification, notificationContainer, false);
            bindNotification(itemView, notification);
            notificationContainer.addView(itemView);
        }

        emptyStateView.setVisibility(notificationContainer.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void loadNotifications() {
        Users currentUser = getActiveUser();
        if (currentUser == null) {
            notifications.clear();
            renderNotifications(notifications);
            return;
        }

        if (currentUser.getId() == null || currentUser.getId().trim().isEmpty()) {
            notifications.clear();
            notifications.addAll(currentUser.getNotifications());
            renderNotifications(notifications);
            return;
        }

        if (notificationListener != null) {
            notificationListener.remove();
        }

        String userId = currentUser.getId();

        notificationListener = db.collection("users")
                .document(userId)
                .collection("notifications")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) {
                        return;
                    }

                    if (value.isEmpty()) {
                        notifications.clear();
                        renderNotifications(notifications);
                        return;
                    }

                    UserNotification[] loadedNotifications = new UserNotification[value.size()];
                    int[] remaining = {value.size()};

                    List<DocumentSnapshot> docs = value.getDocuments();
                    for (int i = 0; i < docs.size(); i++) {
                        final int index = i;
                        DocumentSnapshot doc = docs.get(i);
                        NotificationItem item = doc.toObject(NotificationItem.class);
                        if (item == null) {
                            onNotificationResolved(loadedNotifications, remaining, index, null);
                            continue;
                        }

                        resolveEventName(userId, doc.getId(), item, eventName -> onNotificationResolved(
                                loadedNotifications,
                                remaining,
                                index,
                                toUserNotification(doc.getId(), item, eventName)
                        ));
                    }
                });
    }

    private void onNotificationResolved(UserNotification[] loadedNotifications, int[] remaining, int index,
                                        @Nullable UserNotification notification) {
        loadedNotifications[index] = notification;
        remaining[0]--;

        if (remaining[0] != 0 || !isAdded()) {
            return;
        }

        notifications.clear();
        for (UserNotification loadedNotification : loadedNotifications) {
            if (loadedNotification != null) {
                notifications.add(loadedNotification);
            }
        }
        renderNotifications(notifications);
    }

    private void resolveEventName(@NonNull String userId, @NonNull String notificationId,
                                  @Nullable NotificationItem item, EventNameCallback callback) {
        String storedEventName = item != null ? item.getEventName() : null;
        if (storedEventName != null && !storedEventName.trim().isEmpty()) {
            callback.onResolved(storedEventName);
            return;
        }

        String eventId = item != null ? item.getEventId() : null;
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onResolved("Event update");
            return;
        }

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    String eventName = getDisplayEventName(doc);
                    if (!eventName.equals("Event update")) {
                        db.collection("users")
                                .document(userId)
                                .collection("notifications")
                                .document(notificationId)
                                .update("eventName", eventName);
                    }
                    callback.onResolved(eventName);
                })
                .addOnFailureListener(e -> callback.onResolved("Event update"));
    }

    @NonNull
    private String getDisplayEventName(@NonNull DocumentSnapshot doc) {
        String eventName = doc.getString("name");
        if (eventName == null || eventName.trim().isEmpty()) {
            eventName = doc.getString("eventName");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            eventName = doc.getString("title");
        }
        return eventName == null || eventName.trim().isEmpty() ? "Event update" : eventName.trim();
    }

    private UserNotification toUserNotification(String notificationId, NotificationItem item, String eventName) {
        return new UserNotification(
                parseNotificationType(item.getType()),
                getNotificationTitle(item),
                eventName,
                item.getMessage(),
                item.getEventId(),
                notificationId
        );
    }

    private UserNotification.Type parseNotificationType(@Nullable String type) {
        if ("invitation".equalsIgnoreCase(type)) {
            return UserNotification.Type.INVITATION;
        }
        if ("private_waitlist_invitation".equalsIgnoreCase(type)) {
            return UserNotification.Type.PRIVATE_WAITLIST_INVITATION;
        }
        if ("co_organizer_invitation".equalsIgnoreCase(type)) {
            return UserNotification.Type.CO_ORGANIZER_INVITATION;
        }
        if ("not_selected".equalsIgnoreCase(type) || "rejected".equalsIgnoreCase(type)) {
            return UserNotification.Type.NOT_SELECTED;
        }
        return UserNotification.Type.WAITLISTED;
    }

    private String getNotificationTitle(NotificationItem item) {
        String title = item.getTitle();
        return title == null || title.trim().isEmpty() ? "Notification" : title;
    }

    /**
     * Binds notification data to an inflated view and sets up click listeners for actions.
     *
     * @param itemView The view representing a single notification item.
     * @param notification The notification data to bind.
     */
    private void bindNotification(View itemView, UserNotification notification) {
        View header = itemView.findViewById(R.id.notificationHeader);
        LinearLayout details = itemView.findViewById(R.id.notificationDetails);
        LinearLayout actions = itemView.findViewById(R.id.notificationActions);
        ImageView icon = itemView.findViewById(R.id.ivNotificationIcon);
        ImageView chevron = itemView.findViewById(R.id.ivNotificationChevron);
        TextView title = itemView.findViewById(R.id.tvNotificationTitle);
        TextView eventName = itemView.findViewById(R.id.tvNotificationEvent);
        TextView message = itemView.findViewById(R.id.tvNotificationMessage);
        View declineButton = itemView.findViewById(R.id.btnDeclineInvitation);
        View acceptButton = itemView.findViewById(R.id.btnAcceptInvitation);

        title.setText(notification.getTitle());
        eventName.setText(notification.getEventName());
        message.setText(notification.getMessage());

        header.setBackgroundColor(ContextCompat.getColor(requireContext(), getBackgroundColor(notification.getType())));
        icon.setImageResource(getIcon(notification.getType()));
        actions.setVisibility(isActionableNotification(notification.getType()) ? View.VISIBLE : View.GONE);

        View.OnClickListener toggleListener = v -> {
            boolean isExpanded = details.getVisibility() == View.VISIBLE;
            details.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            chevron.setRotation(isExpanded ? 0f : 180f);
        };

        header.setOnClickListener(toggleListener);
        chevron.setOnClickListener(toggleListener);
        title.setOnClickListener(toggleListener);

        declineButton.setOnClickListener(v -> handleActionableNotificationResponse(notification, false));
        acceptButton.setOnClickListener(v -> handleActionableNotificationResponse(notification, true));
    }

    private void bindWaitlistGroup(View itemView, List<UserNotification> waitlistedNotifications) {
        View header = itemView.findViewById(R.id.waitlistGroupHeader);
        LinearLayout details = itemView.findViewById(R.id.waitlistGroupDetails);
        LinearLayout waitlistItemsContainer = itemView.findViewById(R.id.waitlistItemsContainer);
        ImageView chevron = itemView.findViewById(R.id.ivWaitlistGroupChevron);
        TextView title = itemView.findViewById(R.id.tvWaitlistGroupTitle);

        title.setText("Event waitlisted");
        waitlistItemsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (UserNotification notification : waitlistedNotifications) {
            View entryView = inflater.inflate(R.layout.item_waitlist_notification_entry, waitlistItemsContainer, false);
            TextView eventName = entryView.findViewById(R.id.tvWaitlistEntryEvent);
            TextView message = entryView.findViewById(R.id.tvWaitlistEntryMessage);
            View waitButton = entryView.findViewById(R.id.btnWaitlistContinue);
            View declineButton = entryView.findViewById(R.id.btnWaitlistDecline);

            eventName.setText(notification.getEventName());
            message.setText(notification.getMessage());

            waitButton.setOnClickListener(v -> handleWaitlistResponse(notification, false));
            declineButton.setOnClickListener(v -> handleWaitlistResponse(notification, true));
            waitlistItemsContainer.addView(entryView);
        }

        View.OnClickListener toggleListener = v -> {
            boolean isExpanded = details.getVisibility() == View.VISIBLE;
            details.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            chevron.setRotation(isExpanded ? 0f : 180f);
        };

        header.setOnClickListener(toggleListener);
        chevron.setOnClickListener(toggleListener);
        title.setOnClickListener(toggleListener);
    }

    private boolean isActionableNotification(UserNotification.Type type) {
        return type == UserNotification.Type.INVITATION
                || type == UserNotification.Type.PRIVATE_WAITLIST_INVITATION
                || type == UserNotification.Type.CO_ORGANIZER_INVITATION;
    }

    private void handleActionableNotificationResponse(UserNotification notification, boolean accept) {
        Users currentUser = getActiveUser();
        if (currentUser == null) {
            return;
        }

        if (currentUser.getId() != null
                && notification.getNotificationId() != null
                && notification.getEventId() != null
                && !notification.getEventId().trim().isEmpty()) {
            updateFirestoreActionableNotification(currentUser, notification, accept);
            return;
        }

        boolean handled = notification.getType() == UserNotification.Type.INVITATION
                && (accept
                ? invitationResponseController.acceptInvitation(currentUser, notification)
                : invitationResponseController.declineInvitation(currentUser, notification));
        if (handled) {
            notifications.clear();
            notifications.addAll(currentUser.getNotifications());
            renderNotifications(notifications);
        }
    }

    private void updateFirestoreActionableNotification(Users currentUser, UserNotification notification, boolean accept) {
        if (notification.getType() == UserNotification.Type.PRIVATE_WAITLIST_INVITATION) {
            updateFirestorePrivateWaitlistInvitation(currentUser, notification, accept);
            return;
        }
        if (notification.getType() == UserNotification.Type.CO_ORGANIZER_INVITATION) {
            updateFirestoreCoOrganizerInvitation(currentUser, notification, accept);
            return;
        }
        updateFirestoreInvitation(currentUser, notification, accept);
    }

    private void updateFirestoreInvitation(Users currentUser, UserNotification notification, boolean accept) {
        db.collection("events")
                .document(notification.getEventId())
                .collection("entrants")
                .whereEqualTo("userId", currentUser.getId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    if (!querySnapshot.isEmpty()) {
                        String status = accept ? "ACCEPTED" : "DECLINED";
                        int statusCode = accept ? 2 : 3;
                        batch.update(querySnapshot.getDocuments().get(0).getReference(),
                                "status", status,
                                "statusCode", statusCode);
                        // Update event history status
                        EventCleanupHelper.updateHistoryStatus(
                                currentUser.getId(), notification.getEventId(), status);
                    }

                    batch.delete(db.collection("users")
                            .document(currentUser.getId())
                            .collection("notifications")
                            .document(notification.getNotificationId()));

                    batch.commit().addOnSuccessListener(unused -> {
                        if (accept) {
                            currentUser.acceptInvitation(notification.getEventName());
                        } else {
                            currentUser.declineInvitation(notification.getEventName());
                        }
                    });
                });
    }

    private void updateFirestorePrivateWaitlistInvitation(Users currentUser, UserNotification notification, boolean accept) {
        db.collection("events")
                .document(notification.getEventId())
                .collection("entrants")
                .whereEqualTo("userId", currentUser.getId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot entrantDoc = querySnapshot.getDocuments().get(0);
                        if (accept) {
                            batch.update(entrantDoc.getReference(), "status", "APPLIED", "statusCode", 0);
                        } else {
                            batch.delete(entrantDoc.getReference());
                        }
                    }

                    batch.delete(db.collection("users")
                            .document(currentUser.getId())
                            .collection("notifications")
                            .document(notification.getNotificationId()));

                    batch.commit().addOnSuccessListener(unused -> {
                        if (accept) {
                            currentUser.addWaitlistedEvent(notification.getEventName());
                            writeHistoryRecordForUser(currentUser.getId(), notification.getEventId(), "APPLIED");
                        } else {
                            currentUser.declineInvitation(notification.getEventName());
                        }
                    });
                });
    }

    private void updateFirestoreCoOrganizerInvitation(Users currentUser, UserNotification notification, boolean accept) {
        db.collection("events")
                .document(notification.getEventId())
                .collection("entrants")
                .whereEqualTo("userId", currentUser.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    if (accept) {
                        batch.update(
                                db.collection("events").document(notification.getEventId()),
                                "coOrganizerIds", FieldValue.arrayUnion(currentUser.getId()),
                                "pendingCoOrganizerIds", FieldValue.arrayRemove(currentUser.getId())
                        );
                        for (DocumentSnapshot entrantDoc : querySnapshot.getDocuments()) {
                            batch.delete(entrantDoc.getReference());
                        }
                    } else {
                        batch.update(
                                db.collection("events").document(notification.getEventId()),
                                "pendingCoOrganizerIds", FieldValue.arrayRemove(currentUser.getId())
                        );
                    }

                    batch.delete(db.collection("users")
                            .document(currentUser.getId())
                            .collection("notifications")
                            .document(notification.getNotificationId()));

                    batch.commit().addOnSuccessListener(unused -> {
                        if (accept) {
                            writeHistoryRecordForUser(currentUser.getId(), notification.getEventId(), "ORGANIZED");
                        }
                    });
                });
    }

    private void handleWaitlistResponse(UserNotification notification, boolean decline) {
        Users currentUser = getActiveUser();
        if (currentUser == null) {
            return;
        }

        if (!decline) {
            return;
        }

        if (currentUser.getId() != null
                && notification.getNotificationId() != null
                && notification.getEventId() != null
                && !notification.getEventId().trim().isEmpty()) {
            updateFirestoreWaitlist(currentUser, notification, decline);
            return;
        }

        currentUser.removeNotification(notification);
        currentUser.removeWaitlistedEvent(notification.getEventName());
        notifications.clear();
        notifications.addAll(currentUser.getNotifications());
        renderNotifications(notifications);
    }

    private void updateFirestoreWaitlist(Users currentUser, UserNotification notification, boolean decline) {
        db.collection("events")
                .document(notification.getEventId())
                .collection("entrants")
                .whereEqualTo("userId", currentUser.getId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    if (!querySnapshot.isEmpty()) {
                        batch.delete(querySnapshot.getDocuments().get(0).getReference());
                    }

                    batch.delete(db.collection("users")
                            .document(currentUser.getId())
                            .collection("notifications")
                            .document(notification.getNotificationId()));

                    batch.commit().addOnSuccessListener(unused ->
                            currentUser.removeWaitlistedEvent(notification.getEventName()));
                });
    }

    private Users getActiveUser() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        return currentUser != null ? currentUser : UserSession.getCurrentUser();
    }

    private void writeHistoryRecordForUser(@Nullable String userId, @Nullable String eventId, @NonNull String status) {
        if (userId == null || userId.trim().isEmpty() || eventId == null || eventId.trim().isEmpty()) {
            return;
        }

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        return;
                    }
                    EventCleanupHelper.writeHistoryRecord(userId, eventId, buildHistoryData(eventDoc), status);
                });
    }

    @NonNull
    private Map<String, Object> buildHistoryData(@NonNull DocumentSnapshot eventDoc) {
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("id", valueOrDefault(eventDoc.getString("id"), eventDoc.getId()));
        historyData.put("name", valueOrDefault(eventDoc.getString("name"), ""));
        historyData.put("amount", eventDoc.getLong("amount") != null ? eventDoc.getLong("amount") : 0L);
        historyData.put("description", valueOrDefault(eventDoc.getString("description"), ""));
        historyData.put("event_date", valueOrDefault(eventDoc.getString("event_date"), ""));
        historyData.put("registration_start", valueOrDefault(eventDoc.getString("registration_start"), ""));
        historyData.put("registration_end", valueOrDefault(eventDoc.getString("registration_end"), ""));
        historyData.put("posterUrl", valueOrDefault(eventDoc.getString("posterUrl"), ""));
        historyData.put("sampleSize", eventDoc.getLong("sampleSize") != null ? eventDoc.getLong("sampleSize") : 0L);
        historyData.put("isPrivate", Boolean.TRUE.equals(eventDoc.getBoolean("isPrivate")));

        List<String> coOrganizerIds = FirestoreDataUtils.getStringList(eventDoc, "coOrganizerIds");
        historyData.put("coOrganizerIds", coOrganizerIds != null ? new ArrayList<>(coOrganizerIds) : new ArrayList<String>());

        List<String> pendingCoOrganizerIds = FirestoreDataUtils.getStringList(eventDoc, "pendingCoOrganizerIds");
        historyData.put("pendingCoOrganizerIds",
                pendingCoOrganizerIds != null ? new ArrayList<>(pendingCoOrganizerIds) : new ArrayList<String>());

        String createdBy = eventDoc.getString("createdBy");
        if (createdBy != null && !createdBy.trim().isEmpty()) {
            historyData.put("createdBy", createdBy);
        }

        return historyData;
    }

    @NonNull
    private String valueOrDefault(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    /**
     * Returns the background color resource ID based on the notification type.
     *
     * @param type The type of the notification.
     * @return The color resource ID.
     */
    @ColorRes
    private int getBackgroundColor(UserNotification.Type type) {
        switch (type) {
            case INVITATION:
            case PRIVATE_WAITLIST_INVITATION:
            case CO_ORGANIZER_INVITATION:
                return R.color.notification_green;
            case NOT_SELECTED:
                return R.color.notification_red;
            case WAITLISTED:
            default:
                return R.color.notification_grey;
        }
    }

    /**
     * Returns the icon resource ID based on the notification type.
     *
     * @param type The type of the notification.
     * @return The icon resource ID.
     */
    @DrawableRes
    private int getIcon(UserNotification.Type type) {
        switch (type) {
            case INVITATION:
                return android.R.drawable.checkbox_on_background;
            case PRIVATE_WAITLIST_INVITATION:
                return android.R.drawable.ic_input_add;
            case CO_ORGANIZER_INVITATION:
                return android.R.drawable.ic_menu_manage;
            case NOT_SELECTED:
                return android.R.drawable.ic_delete;
            case WAITLISTED:
            default:
                return android.R.drawable.ic_menu_recent_history;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private interface EventNameCallback {
        void onResolved(String eventName);
    }
}
