package com.example.eventsapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller class responsible for managing notifications related to the event lottery process.
 * It handles notifying users when they are waitlisted, selected, or not selected for an event.
 */
public class LotteryNotificationController {

    /**
     * Notifies a user that they have been placed on the waitlist for an event.
     *
     * @param user The user to notify.
     * @param eventName The name of the event.
     */
    public void notifyWaitlisted(Users user, String eventName) {
        user.addWaitlistedEvent(eventName);
        addNotification(
                user,
                new UserNotification(
                        UserNotification.Type.WAITLISTED,
                        "Event waitlisted",
                        eventName,
                        "You are still on the waitlist for " + eventName + "."
                )
        );
    }

    /**
     * Notifies a user that they have been selected from the waitlist for an event.
     *
     * @param user The user to notify.
     * @param eventName The name of the event.
     */
    public void notifyChosenFromWaitlist(Users user, String eventName) {
        user.addInvitedEvent(eventName);
        addNotification(
                user,
                new UserNotification(
                        UserNotification.Type.INVITATION,
                        "Event Invitation",
                        eventName,
                        "You were selected from the waitlist for " + eventName + "."
                )
        );
    }

    /**
     * Notifies a user that they were not selected from the waitlist for an event.
     *
     * @param user The user to notify.
     * @param eventName The name of the event.
     */
    public void notifyNotChosenFromWaitlist(Users user, String eventName) {
        user.removeWaitlistedEvent(eventName);
        addNotification(
                user,
                new UserNotification(
                        UserNotification.Type.NOT_SELECTED,
                        "Removed from Event",
                        eventName,
                        "You were not selected from the waitlist for " + eventName + "."
                )
        );
    }

    /**
     * Performs selection from a list of entrants, treating groups as a single entity.
     *
     * @param allEntrants The list of all entrants for the event.
     * @param targetCapacity The maximum number of "entities" (individuals or groups) to select.
     * @param occupiedCount The number of spots already taken (invited/accepted).
     * @return A list of Entrants selected by the lottery.
     */
    public List<Entrant> performSelection(List<Entrant> allEntrants, int targetCapacity, int occupiedCount) {
        List<Object> candidates = new ArrayList<>(); // Can contain Entrant or List<Entrant>
        Map<String, List<Entrant>> groupMap = new HashMap<>();

        for (Entrant e : allEntrants) {
            if (e.getStatus() == Entrant.Status.APPLIED) {
                String groupId = e.getGroupId();
                if (groupId != null && !groupId.isEmpty()) {
                    if (!groupMap.containsKey(groupId)) {
                        groupMap.put(groupId, new ArrayList<>());
                        candidates.add(groupMap.get(groupId));
                    }
                    groupMap.get(groupId).add(e);
                } else {
                    candidates.add(e);
                }
            }
        }

        int available = targetCapacity - occupiedCount;
        if (available <= 0 || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        Collections.shuffle(candidates);
        List<Entrant> selectedEntrants = new ArrayList<>();
        int currentCount = 0;

        for (Object candidate : candidates) {
            if (candidate instanceof Entrant) {
                selectedEntrants.add((Entrant) candidate);
                currentCount++;
            } else if (candidate instanceof List) {
                @SuppressWarnings("unchecked")
                List<Entrant> group = (List<Entrant>) candidate;
                selectedEntrants.addAll(group);
                currentCount++; // The entire group counts as one entity
            }
            if (currentCount >= available) break;
        }

        return selectedEntrants;
    }

    /**
     * Helper method to add a notification to a user's account if notifications are enabled.
     *
     * @param user The user to receive the notification.
     * @param notification The notification to add.
     */
    private void addNotification(Users user, UserNotification notification) {
        if (user.isNotificationsEnabled()) {
            user.addNotification(notification);
        }
    }
}
