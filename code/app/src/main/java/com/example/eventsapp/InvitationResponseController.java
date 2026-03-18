package com.example.eventsapp;

/**
 * Controller class responsible for handling user responses to event invitations.
 * It manages the logic for accepting or declining invitations and updating the user's state.
 */
public class InvitationResponseController {

    /**
     * Processes the acceptance of an event invitation.
     * Updates the user's event lists and removes the processed notification.
     *
     * @param user The user responding to the invitation.
     * @param notification The invitation notification being accepted.
     * @return True if the invitation was successfully accepted, false if the notification was not an invitation.
     */
    public boolean acceptInvitation(Users user, UserNotification notification) {
        if (!isInvitation(notification)) {
            return false;
        }

        user.acceptInvitation(notification.getEventName());
        user.removeNotification(notification);
        return true;
    }

    /**
     * Processes the declining of an event invitation.
     * Updates the user's declined invitations list and removes the processed notification.
     *
     * @param user The user responding to the invitation.
     * @param notification The invitation notification being declined.
     * @return True if the invitation was successfully declined, false if the notification was not an invitation.
     */
    public boolean declineInvitation(Users user, UserNotification notification) {
        if (!isInvitation(notification)) {
            return false;
        }

        user.declineInvitation(notification.getEventName());
        user.removeNotification(notification);
        return true;
    }

    /**
     * Helper method to verify if a notification is of type INVITATION.
     *
     * @param notification The notification to check.
     * @return True if it's an invitation, false otherwise.
     */
    private boolean isInvitation(UserNotification notification) {
        return notification != null && notification.getType() == UserNotification.Type.INVITATION;
    }
}
