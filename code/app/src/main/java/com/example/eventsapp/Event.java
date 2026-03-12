package com.example.eventsapp;

import java.io.Serializable;

// Movie object
public class Event implements Serializable {

    // attributes
    private String name;
    private int amount;
    private String registration_start;
    private String registration_end;
    private String event_date;

    // constructor
    public Event(String name, int amount) {
        if (amount == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }
        this.name = name;
        this.amount = amount;
        this.registration_start = registration_start;
        this.registration_end = registration_end;
        this.event_date = event_date;
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

}
