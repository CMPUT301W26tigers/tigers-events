package com.example.eventsapp;

import java.util.Date;

/**
 * Represents a comment object on an event.
 */
public class Comment {
    private String id;
    private String userId;
    private String userName;
    private String text;
    private Date timestamp;

    public Comment() {
    }

    public Comment(String userId, String userName, String text) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.timestamp = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
