package com.example.eventsapp;

public class InvitationResponseController {
    public boolean acceptInvitation(Users user, UserNotification notification) {
        if (!isInvitation(notification)) {
            return false;
        }

        user.acceptInvitation(notification.getEventName());
        user.removeNotification(notification);
        return true;
    }

    public boolean declineInvitation(Users user, UserNotification notification) {
        if (!isInvitation(notification)) {
            return false;
        }

        user.declineInvitation(notification.getEventName());
        user.removeNotification(notification);
        return true;
    }

    private boolean isInvitation(UserNotification notification) {
        return notification != null && notification.getType() == UserNotification.Type.INVITATION;
    }
}
