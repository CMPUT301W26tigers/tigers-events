package com.example.eventsapp;

/**
 * Represents a user who has successfully enrolled in an event.
 *
 * Each EnrolledEntrant contains identifying information about
 * a participant who is confirmed participating in the event.
 *
 * userId: unique identifier of the participant
 * name: participant name
 * email: participant contact email
 * status: enrollment status (typically "Enrolled")
 *
 * Displayed in the recyclerview within EnrolledFragment.
 */
public class EnrolledEntrant {

    private String userId;
    private String name;
    private String email;
    private String status;

    public EnrolledEntrant() {
    }

    /**
     * Creates a new enrolled entrant.
     *
     * @param userId unique identifier of the user
     * @param name participant name
     * @param email participant email
     * @param status enrollment status
     */
    public EnrolledEntrant(String userId, String name, String email, String status) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}