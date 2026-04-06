package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented test for visually testing the removal (cancel) functionality of waitlist entrants.
 * This runs on a device to test the adapter's view recycling, visibility constraints, and
 * edge-case listener detachments without launching the full fragment.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantRemovalVisualTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Apply a standard Material Theme to prevent MaterialCardView inflation crashes
        context.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
    }

    @Test
    public void testCancelButtonVisibilityEdgeCases() {
        // Run on the Main UI Thread to safely manipulate Views
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Entrant> entrants = new ArrayList<>();

            // APPLIED (Waitlisted) -> Organizer hasn't invited them yet. Cancel should be GONE.
            entrants.add(new Entrant("1", "ev1", "Alice", "alice@test.com", Entrant.Status.APPLIED));

            // INVITED -> Organizer CAN cancel this invitation. Cancel should be VISIBLE.
            entrants.add(new Entrant("2", "ev1", "Bob", "bob@test.com", Entrant.Status.INVITED));

            // ACCEPTED -> User already accepted. Cancel should be GONE.
            entrants.add(new Entrant("3", "ev1", "Charlie", "charlie@test.com", Entrant.Status.ACCEPTED));

            // CANCELLED -> Already removed. Cancel should be GONE.
            entrants.add(new Entrant("4", "ev1", "Dave", "dave@test.com", Entrant.Status.CANCELLED));

            // NULL STATUS -> Extreme Edge Case. Should gracefully fallback and be GONE.
            entrants.add(new Entrant("5", "ev1", "Eve", "eve@test.com", null));

            // PRIVATE_INVITED -> Special invite type. By current adapter logic, Cancel should be GONE.
            entrants.add(new Entrant("6", "ev1", "Frank", "frank@test.com", Entrant.Status.PRIVATE_INVITED));

            EntrantAdapter adapter = new EntrantAdapter(entrants);
            FrameLayout parent = new FrameLayout(context);
            EntrantAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

            // Test 1: Applied
            adapter.onBindViewHolder(holder, 0);
            assertEquals("APPLIED status should hide cancel button", View.GONE, holder.ivCancel.getVisibility());

            // Test 2: Invited (Happy Path)
            adapter.onBindViewHolder(holder, 1);
            assertEquals("INVITED status should show cancel button", View.VISIBLE, holder.ivCancel.getVisibility());

            // Test 3: Accepted
            adapter.onBindViewHolder(holder, 2);
            assertEquals("ACCEPTED status should hide cancel button", View.GONE, holder.ivCancel.getVisibility());

            // Test 4: Cancelled
            adapter.onBindViewHolder(holder, 3);
            assertEquals("CANCELLED status should hide cancel button", View.GONE, holder.ivCancel.getVisibility());

            // Test 5: Null
            adapter.onBindViewHolder(holder, 4);
            assertEquals("Null status should safely hide cancel button", View.GONE, holder.ivCancel.getVisibility());

            // Test 6: Private Invited
            adapter.onBindViewHolder(holder, 5);
            assertEquals("PRIVATE_INVITED status should hide cancel button", View.GONE, holder.ivCancel.getVisibility());
        });
    }

    @Test
    public void testCancelEntrantClickListenerAndViewRecycling() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Entrant> entrants = new ArrayList<>();

            Entrant invitedEntrant = new Entrant("1", "ev1", "Removable User", "u1@test.com", Entrant.Status.INVITED);
            Entrant waitlistedEntrant = new Entrant("2", "ev1", "Waitlisted User", "u2@test.com", Entrant.Status.APPLIED);

            entrants.add(invitedEntrant);
            entrants.add(waitlistedEntrant);

            EntrantAdapter adapter = new EntrantAdapter(entrants);
            FrameLayout parent = new FrameLayout(context);
            EntrantAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

            AtomicBoolean listenerFired = new AtomicBoolean(false);
            AtomicReference<Entrant> removedEntrant = new AtomicReference<>(null);

            // Mock the Fragment's listener behavior
            adapter.setOnCancelEntrantListener(entrant -> {
                listenerFired.set(true);
                removedEntrant.set(entrant);
            });

            // uer clicking 'Cancel' on a valid invited entrant ---
            adapter.onBindViewHolder(holder, 0);
            holder.ivCancel.performClick(); // Trigger the click dynamically

            assertTrue("Cancel listener should have been triggered", listenerFired.get());
            assertEquals("The listener should return the exact entrant object being removed",
                    invitedEntrant.getId(), removedEntrant.get().getId());

            // Reset  tracking variables
            listenerFired.set(false);
            removedEntrant.set(null);

            // Edge Case - View Recycling
            // Recycler views reuse the exact same view (holder).
            // must ensure the adapter properly unbinds the 'cancel' click listener.
            adapter.onBindViewHolder(holder, 1); // Bind the non-removable APPLIED entrant to the SAME view

            // The button should naturally be gone
            assertEquals(View.GONE, holder.ivCancel.getVisibility());

            // Even if we force a programmatic click on the hidden UI element, the listener MUST NOT FIRE
            // because the adapter should have set `.setOnClickListener(null)`
            holder.ivCancel.performClick();

            assertFalse("CRITICAL: Listener fired after view recycling! The click listener was not cleared.",
                    listenerFired.get());
        });
    }
}