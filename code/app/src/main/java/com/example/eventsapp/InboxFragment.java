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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

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

        notificationListener = db.collection("users")
                .document(currentUser.getId())
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

                        resolveEventName(item.getEventId(), eventName -> onNotificationResolved(
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

    private void resolveEventName(@Nullable String eventId, EventNameCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onResolved("Event update");
            return;
        }

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    String eventName = doc.getString("name");
                    if (eventName == null || eventName.trim().isEmpty()) {
                        eventName = eventId;
                    }
                    callback.onResolved(eventName);
                })
                .addOnFailureListener(e -> callback.onResolved(eventId));
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
        actions.setVisibility(notification.getType() == UserNotification.Type.INVITATION ? View.VISIBLE : View.GONE);

        View.OnClickListener toggleListener = v -> {
            boolean isExpanded = details.getVisibility() == View.VISIBLE;
            details.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            chevron.setRotation(isExpanded ? 0f : 180f);
        };

        header.setOnClickListener(toggleListener);
        chevron.setOnClickListener(toggleListener);
        title.setOnClickListener(toggleListener);

        declineButton.setOnClickListener(v -> handleInvitationResponse(notification, false));
        acceptButton.setOnClickListener(v -> handleInvitationResponse(notification, true));
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

    private void handleInvitationResponse(UserNotification notification, boolean accept) {
        Users currentUser = getActiveUser();
        if (currentUser == null) {
            return;
        }

        if (currentUser.getId() != null
                && notification.getNotificationId() != null
                && notification.getEventId() != null
                && !notification.getEventId().trim().isEmpty()) {
            updateFirestoreInvitation(currentUser, notification, accept);
            return;
        }

        boolean handled = accept
                ? invitationResponseController.acceptInvitation(currentUser, notification)
                : invitationResponseController.declineInvitation(currentUser, notification);
        if (handled) {
            notifications.clear();
            notifications.addAll(currentUser.getNotifications());
            renderNotifications(notifications);
        }
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
