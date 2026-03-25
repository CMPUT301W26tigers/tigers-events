package com.example.eventsapp;

import java.io.Serializable;

/**
 * Represents an applicant/entrant for an event waitlist.
 * This class tracks the entrant's information and their current status in the selection process.
 */
public class Entrant implements Serializable {

    /**
     * Enumeration of possible statuses for an entrant.
     */
    public enum Status {
        /** Entrant has been privately invited to join the waiting list. */
        PRIVATE_INVITED,
        /** Entrant has applied and is in the pool for sampling. */
        APPLIED,
        /** Entrant has been chosen by sampling and invited to register. */
        INVITED,
        /** Entrant has accepted the invitation. */
        ACCEPTED,
        /** Entrant has declined the invitation. */
        DECLINED,
        /** Entrant's application or invitation has been cancelled. */
        CANCELLED
    }

    private String id;
    private String eventId;
    private String name;
    private String email;
    private String userId;
    private Status status;
    private int statusCode;

    /**
     * Constructs a new Entrant.
     * @param id The unique ID of the entrant. If null, a random UUID is generated.
     * @param eventId The ID of the event the entrant is applying for.
     * @param name The name of the entrant.
     * @param email The email of the entrant.
     * @param status The initial status of the entrant. Defaults to APPLIED if null.
     */
    public Entrant(String id, String eventId, String name, String email, Status status) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.eventId = eventId;
        this.name = name != null ? name : "";
        this.email = email != null ? email : "";
        this.status = status != null ? status : Status.APPLIED;
    }

    /**
     * Gets the entrant ID.
     * @return The ID.
     */
    public String getId() { return id; }

    /**
     * Sets the entrant ID.
     * @param id The ID to set.
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the event ID associated with this entrant.
     * @return The event ID.
     */
    public String getEventId() { return eventId; }

    /**
     * Sets the event ID associated with this entrant.
     * @param eventId The event ID to set.
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Gets the name of the entrant.
     * @return The name.
     */
    public String getName() { return name; }

    /**
     * Sets the name of the entrant.
     * @param name The name to set.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the email of the entrant.
     * @return The email.
     */
    public String getEmail() { return email; }

    /**
     * Sets the email of the entrant.
     * @param email The email to set.
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Gets the Firestore user ID associated with this entrant.
     * @return The user ID.
     */
    public String getUserId() { return userId; }

    /**
     * Sets the Firestore user ID associated with this entrant.
     * @param userId The user ID to set.
     */
    public void setUserId(String userId) { this.userId = userId; }

    /**
     * Gets the current status of the entrant.
     * @return The status.
     */
    public Status getStatus() { return status; }

    /**
     * Sets the status of the entrant.
     * @param status The status to set.
     */
    public void setStatus(Status status) { this.status = status; }

    /**
     * Checks if the entrant has been chosen (status is INVITED or ACCEPTED).
     * @return True if chosen, false otherwise.
     */
    public boolean isChosenInvited() {
        return status == Status.INVITED || status == Status.ACCEPTED;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
