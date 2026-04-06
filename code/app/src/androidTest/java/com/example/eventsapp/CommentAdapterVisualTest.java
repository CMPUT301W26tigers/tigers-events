package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Instrumented test for CommentAdapter to verify UI logic.
 * Specifically checks the visibility of the delete button for different user roles
 * (Admin, Owner, Regular User) and ensures the deletion callback is triggered.
 */
@RunWith(AndroidJUnit4.class)
public class CommentAdapterVisualTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Material theme is required for some components
        context.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
    }

    @Test
    public void testDeleteButtonVisibilityForAdmin() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Comment> comments = new ArrayList<>();
            comments.add(new Comment("user1", "User One", "Test Comment"));

            CommentAdapter adapter = new CommentAdapter(comments);
            adapter.setCurrentUserAccountType("Admin");
            adapter.setEventCreatorId("owner1");
            adapter.setCurrentUserId("admin1");

            FrameLayout parent = new FrameLayout(context);
            CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holder, 0);

            assertEquals("Delete button should be visible for Admin", View.VISIBLE, holder.btnDelete.getVisibility());
        });
    }

    @Test
    public void testDeleteButtonVisibilityForOwner() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Comment> comments = new ArrayList<>();
            comments.add(new Comment("user1", "User One", "Test Comment"));

            CommentAdapter adapter = new CommentAdapter(comments);
            adapter.setCurrentUserAccountType("Regular");
            adapter.setEventCreatorId("owner1");
            adapter.setCurrentUserId("owner1");

            FrameLayout parent = new FrameLayout(context);
            CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holder, 0);

            assertEquals("Delete button should be visible for Event Owner", View.VISIBLE, holder.btnDelete.getVisibility());
        });
    }

    @Test
    public void testDeleteButtonVisibilityForRegularUser() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Comment> comments = new ArrayList<>();
            comments.add(new Comment("user1", "User One", "Test Comment"));

            CommentAdapter adapter = new CommentAdapter(comments);
            adapter.setCurrentUserAccountType("Regular");
            adapter.setEventCreatorId("owner1");
            adapter.setCurrentUserId("user2");

            FrameLayout parent = new FrameLayout(context);
            CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holder, 0);

            assertEquals("Delete button should be GONE for regular user", View.GONE, holder.btnDelete.getVisibility());
        });
    }

    @Test
    public void testCommentDeletionCallback() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Comment> comments = new ArrayList<>();
            Comment commentToDelete = new Comment("user1", "User One", "Test Comment");
            comments.add(commentToDelete);

            CommentAdapter adapter = new CommentAdapter(comments);
            adapter.setCurrentUserAccountType("Admin"); // Grant permission to see button
            
            final boolean[] deleteTriggered = {false};
            final Comment[] capturedComment = {null};
            
            adapter.setOnCommentDeleteListener(comment -> {
                deleteTriggered[0] = true;
                capturedComment[0] = comment;
            });

            FrameLayout parent = new FrameLayout(context);
            CommentAdapter.CommentViewHolder holder = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holder, 0);

            holder.btnDelete.performClick();

            assertTrue("Delete listener should have been triggered", deleteTriggered[0]);
            assertEquals("Captured comment should match the one in the list", commentToDelete, capturedComment[0]);
        });
    }

    @Test
    public void testHostTagVisibility() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Comment> comments = new ArrayList<>();
            // Comment from the owner
            comments.add(new Comment("owner1", "Owner Name", "I am the owner"));
            // Comment from someone else
            comments.add(new Comment("user2", "User Two", "I am just a guest"));

            CommentAdapter adapter = new CommentAdapter(comments);
            adapter.setEventCreatorId("owner1");

            FrameLayout parent = new FrameLayout(context);
            
            // Test Host Tag for Owner
            CommentAdapter.CommentViewHolder holderOwner = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderOwner, 0);
            assertEquals("Host tag should be VISIBLE for owner's comment", View.VISIBLE, holderOwner.tvHostTag.getVisibility());

            // Test Host Tag for Guest
            CommentAdapter.CommentViewHolder holderGuest = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderGuest, 1);
            assertEquals("Host tag should be GONE for guest's comment", View.GONE, holderGuest.tvHostTag.getVisibility());
        });
    }
}
