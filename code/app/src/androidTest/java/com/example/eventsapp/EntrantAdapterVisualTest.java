package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instrumented test for visually testing the waitlist entrant UI (item_waitlist_entrant.xml).
 * This test runs on a virtual/physical device and forces the adapter into various edge-case
 * scenarios to verify that the UI reacts correctly (Colors, Visibilities, Text formatting)
 * without actually launching the app's Main Activity.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantAdapterVisualTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Apply a standard Material Theme.
        // This is REQUIRED because item_waitlist_entrant.xml uses MaterialCardView,
        // which crashes if inflated in a context without a Material theme.
        context.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
    }

    @Test
    public void testAdapterVisualEdgeCases() {
        // Android UI components must be instantiated and modified on the Main UI Thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {

            List<Entrant> entrants = new ArrayList<>();

            // Standard Waitlisted Entrant
            Entrant normalEntrant = new Entrant("1", "event1", "John Doe", "john@example.com", Entrant.Status.APPLIED);

            // Edge Case - Missing Name & Empty Strings
            Entrant emptyNameEntrant = new Entrant("2", "event1", "   ", "", Entrant.Status.APPLIED);

            // Edge Case - Null values
            Entrant nullEntrant = new Entrant("3", "event1", null, null, Entrant.Status.APPLIED);

            // Formally Invited Entrant
            Entrant invitedEntrant = new Entrant("4", "event1", "Lucky User", "lucky@example.com", Entrant.Status.INVITED);

            // Entrant with geographic location coordinates
            Entrant locationEntrant = new Entrant("5", "event1", "Mapper", "map@example.com", Entrant.Status.APPLIED);
            locationEntrant.setLatitude(45.5017);
            locationEntrant.setLongitude(-73.5673);

            entrants.add(normalEntrant);
            entrants.add(emptyNameEntrant);
            entrants.add(nullEntrant);
            entrants.add(invitedEntrant);
            entrants.add(locationEntrant);

            EntrantAdapter adapter = new EntrantAdapter(entrants);

            // Dummy parent layout required to inflate the XML properly using the themed context
            FrameLayout parent = new FrameLayout(context);


            // Normal Entrant
            EntrantAdapter.ViewHolder holderA = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderA, 0); // Bind Position 0
            assertEquals("John Doe", holderA.tvName.getText().toString());
            assertEquals("J", holderA.tvAvatarInitial.getText().toString()); // Initial check
            assertEquals("Waitlisted", holderA.tvAction.getText().toString()); // Status translation
            assertEquals(View.INVISIBLE, holderA.ivPin.getVisibility()); // No location
            assertEquals(View.GONE, holderA.ivMailIcon.getVisibility()); // Not invited
            assertEquals(View.GONE, holderA.ivCancel.getVisibility()); // Not invited

            // Empty Name (Triggers Fallback)
            EntrantAdapter.ViewHolder holderB = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderB, 1);
            assertEquals("Unknown", holderB.tvName.getText().toString());
            assertEquals("?", holderB.tvAvatarInitial.getText().toString()); // Question mark initial

            // Null Values (Triggers Null Safety)
            EntrantAdapter.ViewHolder holderC = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderC, 2);
            assertEquals("Unknown", holderC.tvName.getText().toString());
            assertEquals("", holderC.tvActionSub.getText().toString());

            // Invited Entrant (Triggers Action Icons)
            EntrantAdapter.ViewHolder holderD = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderD, 3);
            assertEquals("Invited", holderD.tvAction.getText().toString());
            assertEquals(View.VISIBLE, holderD.ivMailIcon.getVisibility()); // Mail icon shown
            assertEquals(View.VISIBLE, holderD.ivCancel.getVisibility()); // Cancel icon shown

            // Location Entrant (Triggers Pin Icon)
            EntrantAdapter.ViewHolder holderE = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderE, 4);
            assertEquals(View.VISIBLE, holderE.ivPin.getVisibility()); // Pin is visible

            // LOTTERY DRAFT STATE (The visual green selection highlight)
            Set<String> draftedIds = new HashSet<>();
            draftedIds.add("1"); // Draft John Doe
            adapter.setTempSelectedIds(draftedIds);

            // Re-bind John Doe to test the green layout swap
            adapter.onBindViewHolder(holderA, 0);
            assertEquals("SELECTED (Draft)", holderA.tvAction.getText().toString());
            assertEquals(Color.parseColor("#4CAF50"), holderA.tvAction.getCurrentTextColor());
            assertEquals(Color.parseColor("#4CAF50"), holderA.cardView.getStrokeColor());
            assertEquals(4, holderA.cardView.getStrokeWidth()); // Border thickness applied
        });
    }

    @Test
    public void testAdapterClickListeners() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<Entrant> entrants = new ArrayList<>();
            Entrant mockEntrant = new Entrant("1", "ev", "Tester", "t@t.com", Entrant.Status.INVITED);
            mockEntrant.setLatitude(10.0);
            mockEntrant.setLongitude(20.0);
            entrants.add(mockEntrant);

            EntrantAdapter adapter = new EntrantAdapter(entrants);
            FrameLayout parent = new FrameLayout(context);
            EntrantAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

            // Trackers for our callbacks
            final boolean[] locationClicked = {false};
            final boolean[] cancelClicked = {false};

            adapter.setOnViewLocationListener(entrant -> locationClicked[0] = true);
            adapter.setOnCancelEntrantListener(entrant -> cancelClicked[0] = true);

            adapter.onBindViewHolder(holder, 0);

            // Simulate UI clicks on the interactive icons
            holder.ivPin.performClick();
            holder.ivCancel.performClick();

            assertTrue("Location listener should have been triggered", locationClicked[0]);
            assertTrue("Cancel listener should have been triggered", cancelClicked[0]);
        });
    }
}