package com.example.eventsapp;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

/**
 * Instrumented test for visually testing the cancelled entrant UI (item_cancelled_entrant.xml).
 * This runs on a virtual/physical device and forces the CancelledEntrantAdapter into various
 * edge-case scenarios to verify that the UI reacts correctly (Status parsing, Fallbacks for
 * missing data, Default visibilities).
 */
@RunWith(AndroidJUnit4.class)
public class CancelledEntrantAdapterVisualTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Apply a standard Material Theme.
        // REQUIRED because item_cancelled_entrant.xml uses MaterialCardView,
        // which will crash if inflated without a Material theme.
        context.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight);
    }

    @Test
    public void testCancelledAdapterVisualEdgeCases() {
        // Android UI components must be instantiated and modified on the Main UI Thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {

            ArrayList<Entrant> entrants = new ArrayList<>();

            // Note: Using an empty string "" for the IDs here to bypass
            // the async Firestore profile picture fetch in the adapter. This ensures
            // test remains perfectly synchronous and doesn't flake out

            // Formally DECLINED Entrant
            Entrant declinedEntrant = new Entrant("", "event1", "Alice Smith", "alice@example.com", Entrant.Status.DECLINED);

            // Formally CANCELLED Entrant (lowercase name to test uppercase initial)
            Entrant cancelledEntrant = new Entrant("", "event1", "bob jones", "bob@example.com", Entrant.Status.CANCELLED);

            // Edge Case - Unknown Status (Should fallback to "Unknown")
            Entrant unknownStatusEntrant = new Entrant("", "event1", "Charlie", "charlie@example.com", Entrant.Status.APPLIED);

            // Edge Case - Blank Name (Triggers Fallbacks)
            Entrant emptyNameEntrant = new Entrant("", "event1", "   ", "blank@example.com", Entrant.Status.CANCELLED);

            // Edge Case - Null Values (Triggers Null Safety)
            Entrant nullEntrant = new Entrant("", "event1", null, null, Entrant.Status.DECLINED);

            entrants.add(declinedEntrant);
            entrants.add(cancelledEntrant);
            entrants.add(unknownStatusEntrant);
            entrants.add(emptyNameEntrant);
            entrants.add(nullEntrant);

            CancelledEntrantAdapter adapter = new CancelledEntrantAdapter(context, entrants);

            // Dummy parent layout required to inflate the XML properly using the themed context
            FrameLayout parent = new FrameLayout(context);


            // Declined Entrant
            CancelledEntrantAdapter.CancelledViewHolder holderA = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderA, 0);
            assertEquals("Alice Smith", holderA.tvName.getText().toString());
            assertEquals("A", holderA.tvAvatarInitial.getText().toString()); // Standard initial
            assertEquals("Declined", holderA.tvStatus.getText().toString()); // Status parsing
            assertEquals("alice@example.com", holderA.tvEmail.getText().toString());
            assertEquals(View.VISIBLE, holderA.tvAvatarInitial.getVisibility()); // Initial visible
            assertEquals(View.GONE, holderA.ivProfilePic.getVisibility()); // Image hidden by default


            // Cancelled Entrant & Lowercase initial check
            CancelledEntrantAdapter.CancelledViewHolder holderB = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderB, 1);
            assertEquals("bob jones", holderB.tvName.getText().toString());
            assertEquals("B", holderB.tvAvatarInitial.getText().toString()); // Ensures .toUpperCase() works
            assertEquals("Cancelled", holderB.tvStatus.getText().toString()); // Status parsing


            // Unknown Status (e.g., passed wrong type to the cancelled list)
            CancelledEntrantAdapter.CancelledViewHolder holderC = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderC, 2);
            assertEquals("Unknown", holderC.tvStatus.getText().toString());


            // Blank Name String
            CancelledEntrantAdapter.CancelledViewHolder holderD = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderD, 3);
            assertEquals("Unknown", holderD.tvName.getText().toString()); // Fallback triggered
            assertEquals("?", holderD.tvAvatarInitial.getText().toString()); // Question mark initial applied


            // Null Values handling
            CancelledEntrantAdapter.CancelledViewHolder holderE = adapter.onCreateViewHolder(parent, 0);
            adapter.onBindViewHolder(holderE, 4);
            assertEquals("Unknown", holderE.tvName.getText().toString()); // Null string handled safely
            assertEquals("?", holderE.tvAvatarInitial.getText().toString());
            assertEquals("No email", holderE.tvEmail.getText().toString()); // Null email handler
        });
    }
}