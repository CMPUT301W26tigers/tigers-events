package com.example.eventsapp;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents an event in the EventsApp system.
 * This class stores event details such as name, capacity, dates, and sampling information.
 */
public class Event implements Serializable {

    private String id;
    private String name;
    private int amount;
    private String registration_start;
    private String registration_end;
    private String event_date;
    private String description;
    private String posterUrl;
    private int sampleSize;  // US 02.05.02: number of attendees to sample/invite
    private String hostId;   // ID of the user who created this event
    private boolean geolocationRequired;
    private transient String entrantStatus; //used for UI display only
    private transient boolean fromHistory; //marks events loaded from user's eventHistory collection

    /**
     * Constructs an Event with basic information.
     * @param name The name of the event.
     * @param amount The maximum capacity/amount for the event. Must not be zero.
     */
    public Event(String name, int amount) {
        this(null, name, amount, "", "", "", "", "", 0);
    }

    public Event(String id, String name, int amount, String registration_start, String registration_end, String event_date, String description, String posterUrl, int sampleSize) {
        if (amount == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.registration_start = registration_start != null ? registration_start : "";
        this.registration_end = registration_end != null ? registration_end : "";
        this.event_date = event_date != null ? event_date : "";
        this.description = description != null ? description : "";
        this.posterUrl = posterUrl != null ? posterUrl : "";
        this.sampleSize = Math.max(0, sampleSize);
    }

    /**
     * Gets the event ID.
     * @return The ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the event ID.
     * @param id The ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the maximum capacity/amount of the event.
     * @return The amount.
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Sets the maximum capacity of the event.
     * @param amount The amount to set. Must not be zero.
     * @throws IllegalArgumentException if amount is zero.
     */
    public void setAmount(int amount) {
        if (amount == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }
        this.amount = amount;
    }

    /**
     * Gets the name of the event.
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event.
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the registration start date.
     * @return The start date.
     */
    public String getRegistration_start() {
        return registration_start;
    }

    /**
     * Gets the registration end date.
     * @return The end date.
     */
    public String getRegistration_end() {
        return registration_end;
    }

    /**
     * Gets the event date.
     * @return The event date.
     */
    public String getEvent_date() {
        return event_date;
    }

    /**
     * Sets the registration start date.
     * @param registration_start The start date to set.
     */
    public void setRegistration_start(String registration_start) {
        this.registration_start = registration_start != null ? registration_start : "";
    }

    /**
     * Sets the registration end date.
     * @param registration_end The end date to set.
     */
    public void setRegistration_end(String registration_end) {
        this.registration_end = registration_end != null ? registration_end : "";
    }

    /**
     * Sets the event date.
     * @param event_date The event date to set.
     */
    public void setEvent_date(String event_date) {
        this.event_date = event_date != null ? event_date : "";
    }

    /**
     * Gets the event description.
     * @return The description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    /**
     * Gets the URL of the event's poster.
     * @return The poster URL.
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Sets the URL of the event's poster.
     * @param posterUrl The URL to set.
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl != null ? posterUrl : "";
    }

    /**
     * Gets the sample size (number of attendees to invite).
     * @return The sample size.
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * Sets the sample size for the event lottery.
     * @param sampleSize The size to set.
     */
    public void setSampleSize(int sampleSize) {
        this.sampleSize = Math.max(0, sampleSize);
    }

    /**
     * Gets the ID of the user who created this event.
     * @return The host user ID.
     */
    public String getHostId() {
        return hostId;
    }

    /**
     * Sets the ID of the user who created this event.
     * @param hostId The host user ID.
     */
    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    /**
     * Whether entrants must grant location to join the waitlist.
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    /**
     * Gets the entrant status for the current user (transient, not stored in Firestore).
     * @return The entrant status string, or null if not set.
     */
    public String getEntrantStatus() {
        return entrantStatus;
    }

    /**
     * Sets the entrant status for the current user (transient, used for UI display only).
     * @param entrantStatus The status to set (e.g., APPLIED, INVITED, ACCEPTED, DECLINED, CANCELLED).
     */
    public void setEntrantStatus(String entrantStatus) {
        this.entrantStatus = entrantStatus;
    }

    /**
     * Returns whether this event was loaded from the user's eventHistory (expired event).
     * @return true if from history, false if from the active events collection.
     */
    public boolean isFromHistory() {
        return fromHistory;
    }

    /**
     * Sets whether this event was loaded from the user's eventHistory.
     * @param fromHistory true if loaded from history.
     */
    public void setFromHistory(boolean fromHistory) {
        this.fromHistory = fromHistory;
    }

    /**
     * Generates a deep link URL for this event, used in QR codes.
     * @return The deep link URL.
     */
    public String getEventDeepLink() {
        return "tigers-events://event/" + id;
    }

    /**
     * Returns the event date formatted as "March 27, 2026" instead of "2026-03-27".
     * Falls back to the raw date string if parsing fails.
     * @return The formatted date string, or "No date" if event_date is empty.
     */
    public String getFormattedEventDate() {
        if (event_date == null || event_date.isEmpty()) return "No date";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA);
            input.setLenient(false);
            Date date = input.parse(event_date);
            if (date == null) return event_date;
            SimpleDateFormat output = new SimpleDateFormat("MMMM d, yyyy", Locale.CANADA);
            return output.format(date);
        } catch (ParseException e) {
            return event_date;
        }
    }
}
