package com.example.eventsapp;

import java.io.Serializable;

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

    public Event(String name, int amount) {
        this(null, name, amount, "", "", 0);
    }

    public Event(String id, String name, int amount, String description, String posterUrl, int sampleSize) {
        if (amount == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.name = name;
        this.amount = amount;
        this.registration_start = registration_start;
        this.registration_end = registration_end;
        this.event_date = event_date;
        this.description = description != null ? description : "";
        this.posterUrl = posterUrl != null ? posterUrl : "";
        this.sampleSize = Math.max(0, sampleSize);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        if (amount == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistration_start() {
        return registration_start;
    }
    public String getRegistration_end() {
        return registration_end;
    }
    public String getEvent_date() {
        return event_date;
    }
    public void setRegistration_start(String registration_start) {
        this.registration_start = registration_start;
    }
    public void setRegistration_end(String registration_end) {
        this.registration_end = registration_end;
    }
    public void setEvent_date(String event_date) {
        this.event_date = event_date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl != null ? posterUrl : "";
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = Math.max(0, sampleSize);
    }

    /** Deep link URL for QR code: tigers-events://event/{id} */
    public String getEventDeepLink() {
        return "tigers-events://event/" + id;
    }
}
