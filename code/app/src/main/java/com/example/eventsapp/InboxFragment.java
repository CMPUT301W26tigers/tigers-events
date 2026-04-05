package com.example.eventsapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    /**
     * Resolves the active user and attaches a real-time Firestore listener to their
     * {@code notifications} sub-collection. For each document, the associated event name
     * is looked up (and cached back to Firestore) before the raw data is converted to a
     * {@link UserNotification} and rendered. Falls back to the in-memory notification list
     * when the user has no Firestore ID.
     */
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

                        String groupId = doc.getString("groupId");
                        resolveEventName(userId, doc.getId(), item, eventName -> onNotificationResolved(
                                loadedNotifications,
                                remaining,
                                index,
                                toUserNotification(doc.getId(), item, eventName, groupId)
                        ));
                    }
                });
    }

    /**
     * Called after a single notification's event-name look-up completes. Stores the result
     * at the correct position in the shared array; once all outstanding resolutions have
     * returned, rebuilds and renders the full notification list.
     *
     * @param loadedNotifications Shared array accumulating resolved {@link UserNotification} objects.
     * @param remaining           Single-element counter tracking how many resolutions are still pending.
     * @param index               The position in {@code loadedNotifications} for this result.
     * @param notification        The resolved notification, or {@code null} if resolution failed.
     */
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

    /**
     * Ensures the notification has a human-readable event name. If the name is already
     * stored on the {@link NotificationItem} it is returned immediately via the callback.
     * Otherwise the event document is fetched from Firestore and the resolved name is
     * written back to the notification document as a cache so future reads skip the lookup.
     *
     * @param userId         The current user's Firestore document ID.
     * @param notificationId The Firestore document ID of the notification being resolved.
     * @param item           The raw notification data (may be {@code null}).
     * @param callback       Invoked on the main thread with the resolved event name (never {@code null}).
     */
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

    /**
     * Extracts a non-empty event name from a Firestore document snapshot, trying the fields
     * {@code name}, {@code eventName}, and {@code title} in order.
     *
     * @param doc The Firestore document snapshot for an event.
     * @return A trimmed event name, or {@code "Event update"} if none of the fields are populated.
     */
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

    private UserNotification toUserNotification(String notificationId, NotificationItem item, String eventName, String groupId) {
        return new UserNotification(
                parseNotificationType(item.getType()),
                getNotificationTitle(item),
                eventName,
                item.getMessage(),
                item.getEventId(),
                notificationId,
                groupId
        );
    }

    /**
     * Maps a raw type string from a Firestore notification document to the corresponding
     * {@link UserNotification.Type} enum value. Unrecognised strings default to
     * {@link UserNotification.Type#WAITLISTED}.
     *
     * @param type The raw type string (e.g., {@code "invitation"}, {@code "not_selected"}).
     * @return The matching {@link UserNotification.Type}.
     */
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
        if ("group_waitlist_invitation".equalsIgnoreCase(type)) {
            return UserNotification.Type.GROUP_WAITLIST_INVITATION;
        }
        return UserNotification.Type.WAITLISTED;
    }

    /**
     * Returns the notification title from the raw item, substituting the generic string
     * {@code "Notification"} when the stored title is absent or blank.
     *
     * @param item The raw notification data from Firestore.
     * @return A non-empty title string suitable for display.
     */
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

    /**
     * Binds a list of waitlist notifications into a single grouped card view. All
     * {@link UserNotification.Type#WAITLISTED} notifications are collapsed under one
     * expandable header; each entry exposes "Stay on waitlist" and "Leave waitlist" actions.
     *
     * @param itemView                The inflated group card view.
     * @param waitlistedNotifications The list of waitlist notifications to render inside the group.
     */
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

    /**
     * Returns {@code true} for notification types that require an explicit Accept/Decline
     * response (any kind of invitation). Waitlist and informational notifications are
     * not considered actionable in this sense.
     *
     * @param type The notification type to evaluate.
     * @return {@code true} if the notification should display Accept/Decline action buttons.
     */
    private boolean isActionableNotification(UserNotification.Type type) {
        return type == UserNotification.Type.INVITATION
                || type == UserNotification.Type.PRIVATE_WAITLIST_INVITATION
                || type == UserNotification.Type.CO_ORGANIZER_INVITATION
                || type == UserNotification.Type.GROUP_WAITLIST_INVITATION;
    }

    /**
     * Dispatches an Accept or Decline response for an actionable notification.
     * If the notification has a Firestore-backed ID, the response is persisted remotely
     * via the appropriate update method; otherwise it falls back to the in-memory
     * {@link InvitationResponseController}.
     *
     * @param notification The notification the user responded to.
     * @param accept       {@code true} if the user accepted; {@code false} if declined.
     */
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

    /**
     * Routes the user's Accept/Decline response to the correct Firestore update method
     * based on the notification's type (regular invitation, private-waitlist invitation,
     * or co-organizer invitation).
     *
     * @param currentUser  The authenticated user responding to the notification.
     * @param notification The actionable notification being acted upon.
     * @param accept       {@code true} to accept; {@code false} to decline.
     */
    private void updateFirestoreActionableNotification(Users currentUser, UserNotification notification, boolean accept) {
        if (notification.getType() == UserNotification.Type.PRIVATE_WAITLIST_INVITATION) {
            updateFirestorePrivateWaitlistInvitation(currentUser, notification, accept);
            return;
        }
        if (notification.getType() == UserNotification.Type.CO_ORGANIZER_INVITATION) {
            updateFirestoreCoOrganizerInvitation(currentUser, notification, accept);
            return;
        }
        if (notification.getType() == UserNotification.Type.GROUP_WAITLIST_INVITATION) {
            updateFirestoreGroupWaitlistInvitation(currentUser, notification, accept);
            return;
        }
        updateFirestoreInvitation(currentUser, notification, accept);
    }

    /**
     * Persists an Accept or Decline response for a standard event invitation. Updates the
     * entrant's {@code status} in the event's {@code entrants} sub-collection, adds the
     * user to the {@code enrolled} sub-collection on acceptance, and deletes the
     * notification document on completion. Silently dismisses stale invitations for
     * entrants already cancelled by the organiser.
     *
     * @param currentUser  The authenticated user responding.
     * @param notification The invitation notification being resolved.
     * @param accept       {@code true} to accept the invitation; {@code false} to decline.
     */
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
                        DocumentSnapshot entrantDoc = querySnapshot.getDocuments().get(0);

                        String currentStatus = entrantDoc.getString("status");
                        // Prevent accepting/declining if the organizer already removed them
                        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
                            batch.delete(db.collection("users")
                                    .document(currentUser.getId())
                                    .collection("notifications")
                                    .document(notification.getNotificationId()));
                            batch.commit().addOnSuccessListener(unused -> {
                                if (getContext() != null) {
                                    android.widget.Toast.makeText(getContext(), "This invitation is no longer valid.", android.widget.Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }

                        String status = accept ? "ACCEPTED" : "DECLINED";
                        int statusCode = accept ? 2 : 3;
                        batch.update(entrantDoc.getReference(),
                                "status", status,
                                "statusCode", statusCode);
                        if (accept) {
                            Map<String, Object> enrolledData = new HashMap<>();
                            enrolledData.put("userId", currentUser.getId());
                            enrolledData.put("name", entrantDoc.getString("name"));
                            enrolledData.put("email", entrantDoc.getString("email"));
                            enrolledData.put("status", "Accepted");
                            batch.set(db.collection("events")
                                            .document(notification.getEventId())
                                            .collection("enrolled")
                                            .document(entrantDoc.getId()),
                                    enrolledData);
                        }
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

    /**
     * Persists an Accept or Decline response for a private-waitlist invitation. Accepting
     * transitions the entrant's status to {@code APPLIED} (joining the public waitlist);
     * declining removes the entrant document entirely. The notification document is
     * deleted in both cases.
     *
     * @param currentUser  The authenticated user responding.
     * @param notification The private-waitlist invitation notification.
     * @param accept       {@code true} to join the waitlist; {@code false} to decline the invite.
     */
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

    private void updateFirestoreGroupWaitlistInvitation(Users currentUser, UserNotification notification, boolean accept) {
        String groupId = notification.getGroupId();
        if (groupId == null) return;

        db.collection("events").document(notification.getEventId())
                .collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        WriteBatch batch = db.batch();
                        if (accept) {
                            batch.update(doc.getReference(), "acceptedEmails", FieldValue.arrayUnion(currentUser.getEmail()));
                            
                            // Check if everyone accepted
                            List<String> memberEmails = (List<String>) doc.get("memberEmails");
                            List<String> acceptedEmails = (List<String>) doc.get("acceptedEmails");
                            // Add current user to local list for comparison
                            if (acceptedEmails == null) acceptedEmails = new ArrayList<>();
                            if (!acceptedEmails.contains(currentUser.getEmail())) {
                                acceptedEmails.add(currentUser.getEmail());
                            }
                            
                            if (memberEmails != null && acceptedEmails != null && acceptedEmails.size() >= memberEmails.size() + 1) { // +1 for creator
                                batch.update(doc.getReference(), "status", "COMPLETED");
                                // Add all members to waitlist
                                addGroupToWaitlist(notification.getEventId(), doc);
                            }
                        } else {
                            batch.update(doc.getReference(), "status", "DECLINED");
                        }

                        batch.delete(db.collection("users")
                                .document(currentUser.getId())
                                .collection("notifications")
                                .document(notification.getNotificationId()));
                        
                        batch.commit();
                    }
                });
    }

    private void addGroupToWaitlist(String eventId, DocumentSnapshot groupDoc) {
        String creatorId = groupDoc.getString("creatorId");
        List<String> memberEmails = (List<String>) groupDoc.get("memberEmails");
        List<String> allEmails = new ArrayList<>();
        allEmails.add(groupDoc.getString("creatorEmail"));
        if (memberEmails != null) allEmails.addAll(memberEmails);

        for (String email : allEmails) {
            db.collection("users").whereEqualTo("email", email).limit(1).get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                            Users user = userDoc.toObject(Users.class);
                            if (user != null) {
                                user.setId(userDoc.getId());
                                writeEntrantToWaitlist(eventId, user);
                            }
                        }
                    });
        }
    }

    private void writeEntrantToWaitlist(String eventId, Users user) {
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("id", user.getId());
        entrantData.put("eventId", eventId);
        entrantData.put("name", user.getName());
        entrantData.put("email", user.getEmail());
        entrantData.put("status", "APPLIED");
        entrantData.put("userId", user.getId());
        entrantData.put("statusCode", 0);
        entrantData.put("locationUpdatedAt", FieldValue.serverTimestamp());

        db.collection("events").document(eventId)
                .collection("entrants").document(user.getId())
                .set(entrantData)
                .addOnSuccessListener(unused -> {
                    new FirestoreNotificationHelper().sendWaitlistedNotification(user.getId(), eventId);
                    writeHistoryRecordForUser(user.getId(), eventId, "APPLIED");
                });
    }

    private void updateFirestoreCoOrganizerInvitation(Users currentUser, UserNotification notification, boolean accept) {
        if (!accept) {
            WriteBatch batch = db.batch();
            batch.update(
                    db.collection("events").document(notification.getEventId()),
                    "pendingCoOrganizerIds", FieldValue.arrayRemove(currentUser.getId())
            );
            batch.delete(db.collection("users")
                    .document(currentUser.getId())
                    .collection("notifications")
                    .document(notification.getNotificationId()));
            batch.commit();
            return;
        }

        db.collection("events")
                .document(notification.getEventId())
                .collection("entrants")
                .whereEqualTo("userId", currentUser.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String currentUserId = currentUser.getId();
                    WriteBatch batch = db.batch();
                    batch.update(
                            db.collection("events").document(notification.getEventId()),
                            "coOrganizerIds", FieldValue.arrayUnion(currentUserId),
                            "pendingCoOrganizerIds", FieldValue.arrayRemove(currentUserId)
                    );

                    for (DocumentSnapshot entrantDoc : querySnapshot.getDocuments()) {
                        batch.delete(entrantDoc.getReference());
                    }

                    batch.delete(db.collection("events")
                            .document(notification.getEventId())
                            .collection("enrolled")
                            .document(currentUserId));

                    batch.delete(db.collection("users")
                            .document(currentUser.getId())
                            .collection("notifications")
                            .document(notification.getNotificationId()));

                    batch.commit().addOnSuccessListener(unused ->
                            writeHistoryRecordForUser(currentUser.getId(), notification.getEventId(), "ORGANIZED"));
                });
    }

    /**
     * Handles the user's decision to stay on or leave a waitlist. If the notification has
     * a Firestore-backed ID the change is persisted remotely; otherwise the in-memory list
     * is updated directly.
     *
     * @param notification The waitlist notification the user acted upon.
     * @param decline      {@code true} if the user chose to leave the waitlist; {@code false} to stay.
     */
    private void handleWaitlistResponse(UserNotification notification, boolean decline) {
        Users currentUser = getActiveUser();
        if (currentUser == null) {
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

    /**
     * Persists the user's decision to leave (or simply dismiss) a waitlist notification.
     * If declining, the entrant document is deleted and the event is removed from the
     * user's history. The notification document is deleted regardless of the outcome.
     *
     * @param currentUser  The authenticated user acting on the notification.
     * @param notification The waitlist notification being resolved.
     * @param decline      {@code true} if the user is leaving the waitlist; {@code false} to
     *                     dismiss the notification without leaving.
     */
    private void updateFirestoreWaitlist(Users currentUser, UserNotification notification, boolean decline) {
        db.collection("events")
                .document(notification.getEventId())
                .collection("entrants")
                .whereEqualTo("userId", currentUser.getId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    if (decline && !querySnapshot.isEmpty()) {
                        batch.delete(querySnapshot.getDocuments().get(0).getReference());
                    }

                    batch.delete(db.collection("users")
                            .document(currentUser.getId())
                            .collection("notifications")
                            .document(notification.getNotificationId()));

                    batch.commit().addOnSuccessListener(unused -> {
                        currentUser.removeNotification(notification);
                        if (decline) {
                            currentUser.removeWaitlistedEvent(notification.getEventName());
                            EventCleanupHelper.deleteHistoryRecord(currentUser.getId(), notification.getEventId());
                        }
                        notifications.clear();
                        notifications.addAll(currentUser.getNotifications());
                        renderNotifications(notifications);
                    });
                });
    }

    /**
     * Returns the currently authenticated user, preferring {@link UserManager} and
     * falling back to the legacy {@link UserSession} for in-memory sessions.
     *
     * @return The active {@link Users} object, or {@code null} if no session exists.
     */
    private Users getActiveUser() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        return currentUser != null ? currentUser : UserSession.getCurrentUser();
    }

    /**
     * Fetches the event document from Firestore and, if it exists, writes or updates an
     * entry in the user's {@code eventHistory} collection via {@link EventCleanupHelper}.
     * Null or blank IDs are silently ignored.
     *
     * @param userId  The Firestore user document ID, or {@code null} to skip.
     * @param eventId The Firestore event document ID, or {@code null} to skip.
     * @param status  The entrant status to record (e.g., {@code "APPLIED"}, {@code "ORGANIZED"}).
     */
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

    /**
     * Constructs a map of event field values suitable for writing to a user's
     * {@code eventHistory} document. All fields are extracted defensively, falling back
     * to safe defaults when values are absent or null.
     *
     * @param eventDoc The Firestore document snapshot of the event.
     * @return A map containing all relevant event fields ready for Firestore persistence.
     */
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

    /**
     * Returns {@code value} when it is non-null and non-blank; otherwise returns {@code fallback}.
     * Intended for safely extracting optional Firestore string fields.
     *
     * @param value    The candidate string (may be {@code null} or blank).
     * @param fallback The default string to use when {@code value} is absent or blank.
     * @return A guaranteed non-null, non-blank string.
     */
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
            case GROUP_WAITLIST_INVITATION:
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
            case GROUP_WAITLIST_INVITATION:
                return android.R.drawable.ic_input_add;
            case WAITLISTED:
            default:
                return android.R.drawable.ic_menu_recent_history;
        }
    }

    /**
     * Removes the active Firestore snapshot listener to prevent memory leaks and stale
     * UI callbacks after the fragment's view hierarchy has been destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    /** Internal callback for asynchronous event-name resolution within this fragment. */
    private interface EventNameCallback {
        /**
         * Invoked on the main thread once the event name has been determined.
         *
         * @param eventName The resolved event name; never {@code null}.
         */
        void onResolved(String eventName);
    }
}
