package com.example.eventsapp;

public class EnrolledEntrant {

    private String userId;
    private String name;
    private String email;
    private String status;

    public EnrolledEntrant() {
    }

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