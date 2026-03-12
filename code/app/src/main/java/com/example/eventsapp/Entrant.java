package com.example.eventsapp;

import java.io.Serializable;

/**
 * Represents an applicant/entrant for an event waitlist.
 * Status: APPLIED (in pool), INVITED (chosen by sampling), ACCEPTED, DECLINED, CANCELLED
 */
public class Entrant implements Serializable {

    public enum Status {
        APPLIED,   // Applied, in pool for sampling
        INVITED,   // Chosen by sampling, invited to register
        ACCEPTED,
        DECLINED,
        CANCELLED
    }

    private String id;
    private String eventId;
    private String name;
    private String email;
    private Status status;

    public Entrant(String id, String eventId, String name, String email, Status status) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.eventId = eventId;
        this.name = name != null ? name : "";
        this.email = email != null ? email : "";
        this.status = status != null ? status : Status.APPLIED;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    /** Chosen entrants = INVITED or ACCEPTED (invited to apply/register) */
    public boolean isChosenInvited() {
        return status == Status.INVITED || status == Status.ACCEPTED;
    }
}
