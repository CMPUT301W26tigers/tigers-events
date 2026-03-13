package com.example.eventsapp;

/**
 * Represents an entrant who is specifically enrolled in an event's list (e.g., waitlist or accepted list).
 * This class is typically used for displaying entrant information in lists or adapters.
 */
public class EnrolledEntrant {

    private String userId;
    private String name;
    private String email;
    private String status;

    /**
     * No-argument constructor required for Firestore deserialization.
     */
    public EnrolledEntrant() {
    }

    /**
     * Constructs an EnrolledEntrant with all details.
     * @param userId The unique ID of the user.
     * @param name The name of the entrant.
     * @param email The email of the entrant.
     * @param status The current status of the entrant for a specific event.
     */
    public EnrolledEntrant(String userId, String name, String email, String status) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.status = status;
    }

    /**
     * Gets the user ID.
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the name of the entrant.
     * @return The entrant's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the email of the entrant.
     * @return The entrant's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the enrollment status.
     * @return The status string.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the user ID.
     * @param userId The user ID to set.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Sets the name of the entrant.
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the email of the entrant.
     * @param email The email to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Sets the enrollment status.
     * @param status The status to set.
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
