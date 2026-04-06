package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented test for visually testing the removal functionality of enrolled entrants.
 * This runs on a device to test the adapter's view bindings, view recycling,
 * edge-case data, and listener detachments without launching the full fragment.
 */
@RunWith(AndroidJUnit4.class)
public class EnrolledEntrantRemovalVisualTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Apply a standard Material Theme to prevent MaterialCardView inflation crashes
        context.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
    }

    @Test
    public void testCancelButtonTriggersListenerCorrectly() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArrayList<Entrant> entrants = new ArrayList<>();
            Entrant enrolledEntrant = new Entrant("1", "ev1", "Alice", "alice@test.com", Entrant.Status.ACCEPTED);
            entrants.add(enrolledEntrant);

            EnrolledEntrantAdapter adapter = new EnrolledEntrantAdapter(context, entrants);
            FrameLayout parent = new FrameLayout(context);
            EnrolledEntrantAdapter.EnrolledViewHolder holder = adapter.onCreateViewHolder(parent, 0);

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            AtomicReference<Entrant> removedEntrant = new AtomicReference<>(null);

            // Mock the Fragment's listener behavior
            adapter.setOnCancelEntrantListener(entrant -> {
                listenerFired.set(true);
                removedEntrant.set(entrant);
            });

            // Bind the view
            adapter.onBindViewHolder(holder, 0);

            // In the enrolled list, the cancel button should ALWAYS be visible
            assertEquals("Cancel button must be visible for enrolled entrants",
                    View.VISIBLE, holder.ivCancel.getVisibility());

            // Simulate the user clicking the remove/cancel button
            holder.ivCancel.performClick();

            assertTrue("Cancel listener should have been triggered", listenerFired.get());
            assertNotNull("Removed entrant should not be null", removedEntrant.get());
            assertEquals("The listener should return the exact entrant object being removed",
                    "1", removedEntrant.get().getId());
        });
    }

    @Test
    public void testListenerUpdatesOnViewRecycling() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArrayList<Entrant> entrants = new ArrayList<>();

            Entrant entrantA = new Entrant("1", "ev1", "User A", "a@test.com", Entrant.Status.ACCEPTED);
            Entrant entrantB = new Entrant("2", "ev1", "User B", "b@test.com", Entrant.Status.ACCEPTED);
            entrants.add(entrantA);
            entrants.add(entrantB);

            EnrolledEntrantAdapter adapter = new EnrolledEntrantAdapter(context, entrants);
            FrameLayout parent = new FrameLayout(context);
            EnrolledEntrantAdapter.EnrolledViewHolder holder = adapter.onCreateViewHolder(parent, 0);

            AtomicReference<Entrant> removedEntrant = new AtomicReference<>(null);
            adapter.setOnCancelEntrantListener(removedEntrant::set);

            // Bind first entrant
            adapter.onBindViewHolder(holder, 0);

            // Edge Case - View Recycling
            // Recycler views reuse the exact same view (holder) when scrolling.
            // Ensuring the adapter updates the lambda reference to the NEW entrant.
            adapter.onBindViewHolder(holder, 1); // Bind entrantB to the SAME view

            // Simulate the user clicking remove
            holder.ivCancel.performClick();

            // If recycling failed, it would incorrectly remove User A (ID: 1)
            assertEquals("CRITICAL: Listener captured the wrong entrant after view recycling!",
                    "2", removedEntrant.get().getId());
        });
    }

    @Test
    public void testNullListenerDoesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArrayList<Entrant> entrants = new ArrayList<>();
            entrants.add(new Entrant("1", "ev1", "Bob", "bob@test.com", Entrant.Status.ACCEPTED));

            EnrolledEntrantAdapter adapter = new EnrolledEntrantAdapter(context, entrants);
            FrameLayout parent = new FrameLayout(context);
            EnrolledEntrantAdapter.EnrolledViewHolder holder = adapter.onCreateViewHolder(parent, 0);

            // INTENTIONAL NOT setting the OnCancelEntrantListener
            adapter.onBindViewHolder(holder, 0);

            try {
                // If the adapter doesn't safely check if (listener != null), this will crash
                holder.ivCancel.performClick();
            } catch (Exception e) {
                fail("App crashed! Clicking remove with a null listener threw an exception: " + e.getMessage());
            }
        });
    }

    @Test
    public void testMalformedDataEdgeCases() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArrayList<Entrant> entrants = new ArrayList<>();
            // EXTREME EDGE CASE: Entrant with null name, null email, and null status
            Entrant badEntrant = new Entrant("99", "ev1", null, null, null);
            entrants.add(badEntrant);

            EnrolledEntrantAdapter adapter = new EnrolledEntrantAdapter(context, entrants);
            FrameLayout parent = new FrameLayout(context);
            EnrolledEntrantAdapter.EnrolledViewHolder holder = adapter.onCreateViewHolder(parent, 0);

            adapter.onBindViewHolder(holder, 0);

            // Verify UI handled null data fallbacks without crashing
            assertEquals("Null name should default to 'Unknown'", "Unknown", holder.tvName.getText().toString());
            assertEquals("Null name avatar should default to '?'", "?", holder.tvAvatarInitial.getText().toString());
            assertEquals("Null email should default to empty string", "", holder.tvEmail.getText().toString());
            assertEquals("Any entrant in this list should visually show ACCEPTED status", "ACCEPTED", holder.tvStatus.getText().toString());

            // Verify removal still works safely for malformed users
            AtomicBoolean listenerFired = new AtomicBoolean(false);
            adapter.setOnCancelEntrantListener(e -> listenerFired.set(true));

            holder.ivCancel.performClick();
            assertTrue("Should successfully remove a malformed user", listenerFired.get());
        });
    }
}