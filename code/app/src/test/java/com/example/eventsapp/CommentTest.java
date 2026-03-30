package com.example.eventsapp;

import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the Comment class.
 */
public class CommentTest {

    @Test
    public void testEmptyConstructor() {
        Comment comment = new Comment();
        assertNull(comment.getId());
        assertNull(comment.getUserId());
        assertNull(comment.getUserName());
        assertNull(comment.getText());
        assertNull(comment.getTimestamp());
    }

    @Test
    public void testParameterizedConstructor() {
        String userId = "user123";
        String userName = "John Doe";
        String text = "This is a test comment.";

        Comment comment = new Comment(userId, userName, text);

        assertEquals(userId, comment.getUserId());
        assertEquals(userName, comment.getUserName());
        assertEquals(text, comment.getText());
        assertNotNull(comment.getTimestamp());
    }

    @Test
    public void testSetAndGetId() {
        Comment comment = new Comment();
        String id = "comment789";
        comment.setId(id);
        assertEquals(id, comment.getId());
    }

    @Test
    public void testSetAndGetUserId() {
        Comment comment = new Comment();
        String userId = "user456";
        comment.setUserId(userId);
        assertEquals(userId, comment.getUserId());
    }

    @Test
    public void testSetAndGetUserName() {
        Comment comment = new Comment();
        String userName = "Jane Smith";
        comment.setUserName(userName);
        assertEquals(userName, comment.getUserName());
    }

    @Test
    public void testSetAndGetText() {
        Comment comment = new Comment();
        String text = "Testing setters and getters.";
        comment.setText(text);
        assertEquals(text, comment.getText());
    }

    @Test
    public void testSetAndGetTimestamp() {
        Comment comment = new Comment();
        Date now = new Date();
        comment.setTimestamp(now);
        assertEquals(now, comment.getTimestamp());
    }
}
