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

    /**
     * No-argument constructor required for Firestore deserialization.
     */
    public Comment() {
    }

    /**
     * Constructs a Comment with author details and content, capturing the current time as its timestamp.
     *
     * @param userId   the Firestore document ID of the commenting user
     * @param userName the display name shown alongside the comment
     * @param text     the body of the comment
     */
    public Comment(String userId, String userName, String text) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.timestamp = new Date();
    }

    /**
     * Returns the Firestore document ID of this comment.
     *
     * @return the comment ID, or {@code null} if not yet persisted
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the Firestore document ID of this comment.
     *
     * @param id the Firestore document ID assigned after persistence
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the Firestore document ID of the user who authored this comment.
     *
     * @return the author's user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the Firestore document ID of the user who authored this comment.
     *
     * @param userId the author's user ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Returns the display name of the user who authored this comment.
     *
     * @return the author's display name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the display name of the user who authored this comment.
     *
     * @param userName the author's display name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns the text body of the comment.
     *
     * @return the comment content
     */
    public String getText() {
        return text;
    }

    /**
     * Replaces the text body of the comment.
     *
     * @param text the new comment content
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the date and time at which this comment was created.
     *
     * @return the creation timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Overrides the creation timestamp of this comment.
     *
     * @param timestamp the date/time to associate with this comment
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
