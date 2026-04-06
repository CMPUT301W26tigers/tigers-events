package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Bundle;
import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.button.MaterialButton;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Instrumented test for visually testing the Waitlist Capacity logic in EventDetailFragment.
 * This ensures that organizers can OPTIONALLY limit the number of entrants.
 *
 * Preventing flaking and avoiding hitting Firebase by initializing with an empty event ID
 * (which stops Firestore queries). Then just injecting different local waitlist capacities, counts,
 * and edge-case states directly into the fragment to test the UI's reaction.
 */
@RunWith(AndroidJUnit4.class)
public class WaitlistCapacityVisualTest {

    // Use MainActivity to host the fragment, utilizing dependencies you already have.
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private EventDetailFragment fragment;

    @Before
    public void setUp() {
        activityRule.getScenario().onActivity(activity -> {
            // Instantiate our fragment manually
            fragment = new EventDetailFragment();

            // Pass an empty eventId to intentionally bypass Firebase data fetching.
            Bundle args = new Bundle();
            args.putString("eventId", "");
            fragment.setArguments(args);

            // Forcefully inject this fragment over whatever MainActivity is currently displaying
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commitNow();
        });
    }

    @Test
    public void testUnlimitedWaitlistCapacity() {
        activityRule.getScenario().onActivity(activity -> {
            try {
                // Capacity = 0 represents unlimited. Waitlist count is high (100).
                setupFragmentState(fragment, 0, 100, false);
                invokeRefreshButtonState(fragment);

                MaterialButton btnWaitlist = fragment.getView().findViewById(R.id.btnWaitlist);

                assertTrue("Button should be ENABLED for unlimited capacity", btnWaitlist.isEnabled());
                assertEquals(fragment.getString(R.string.join_waitlist), btnWaitlist.getText().toString());
            } catch (Exception e) {
                fail("Reflection setup failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testLimitedWaitlistCapacity_NotFull() {
        activityRule.getScenario().onActivity(activity -> {
            try {
                // Capacity = 5, Current Count = 4
                setupFragmentState(fragment, 5, 4, false);
                invokeRefreshButtonState(fragment);

                MaterialButton btnWaitlist = fragment.getView().findViewById(R.id.btnWaitlist);

                assertTrue("Button should be ENABLED when capacity is not yet reached", btnWaitlist.isEnabled());
                assertEquals(fragment.getString(R.string.join_waitlist), btnWaitlist.getText().toString());
            } catch (Exception e) {
                fail("Reflection setup failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testLimitedWaitlistCapacity_ExactlyFull() {
        activityRule.getScenario().onActivity(activity -> {
            try {
                // Capacity = 5, Current Count = 5
                setupFragmentState(fragment, 5, 5, false);
                invokeRefreshButtonState(fragment);

                MaterialButton btnWaitlist = fragment.getView().findViewById(R.id.btnWaitlist);
                MaterialButton btnWaitlist2 = fragment.getView().findViewById(R.id.btnWaitlist2);

                assertFalse("Button should be DISABLED when capacity is reached", btnWaitlist.isEnabled());
                assertEquals(fragment.getString(R.string.waitlist_full), btnWaitlist.getText().toString());

                // Ensure the group join button is also disabled
                assertFalse("Group join button should be DISABLED", btnWaitlist2.isEnabled());
                assertEquals(fragment.getString(R.string.waitlist_full), btnWaitlist2.getText().toString());
            } catch (Exception e) {
                fail("Reflection setup failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testEdgeCase_WaitlistOverCapacity() {
        activityRule.getScenario().onActivity(activity -> {
            try {
                // Edge Case: Organizer lowers capacity AFTER people have joined.
                // Capacity = 5, Current Count = 10
                setupFragmentState(fragment, 5, 10, false);
                invokeRefreshButtonState(fragment);

                MaterialButton btnWaitlist = fragment.getView().findViewById(R.id.btnWaitlist);

                assertFalse("Button should be DISABLED when waitlist is over capacity", btnWaitlist.isEnabled());
                assertEquals(fragment.getString(R.string.waitlist_full), btnWaitlist.getText().toString());
            } catch (Exception e) {
                fail("Reflection setup failed: " + e.getMessage());
            }
        });
    }

    @Test
    public void testEdgeCase_WaitlistFullButUserAlreadyOnWaitlist() {
        activityRule.getScenario().onActivity(activity -> {
            try {
                // Edge Case: The waitlist is full, BUT the user is already on it.
                // They should be allowed to interact with the button to LEAVE the waitlist.
                setupFragmentState(fragment, 5, 5, true);
                invokeRefreshButtonState(fragment);

                MaterialButton btnWaitlist = fragment.getView().findViewById(R.id.btnWaitlist);
                MaterialButton btnWaitlist2 = fragment.getView().findViewById(R.id.btnWaitlist2);

                assertTrue("Button should remain ENABLED so the user can leave", btnWaitlist.isEnabled());
                assertEquals(fragment.getString(R.string.leave_waitlist), btnWaitlist.getText().toString());

                // Group join button should be hidden if they are already on the waitlist
                assertEquals("Group join button should be GONE if already on waitlist",
                        View.GONE, btnWaitlist2.getVisibility());
            } catch (Exception e) {
                fail("Reflection setup failed: " + e.getMessage());
            }
        });
    }

    // --- Helper Methods to access private fields and methods in EventDetailFragment ---

    private void setupFragmentState(EventDetailFragment fragment, int capacity, int count, boolean isOnWaitlist) throws Exception {
        setField(fragment, "waitlistCapacity", capacity);
        setField(fragment, "waitlistCount", count);
        setField(fragment, "isOnWaitlist", isOnWaitlist);
        setField(fragment, "isRegistrationOpen", true);
        setField(fragment, "userIsOrganizer", false);
        setField(fragment, "isPrivateEvent", false);
        setField(fragment, "hasPendingGroup", false);

        if (isOnWaitlist) {
            setField(fragment, "userStatus", "APPLIED");
        } else {
            setField(fragment, "userStatus", null);
        }
    }

    private void invokeRefreshButtonState(EventDetailFragment fragment) throws Exception {
        Method method = EventDetailFragment.class.getDeclaredMethod("refreshWaitlistButtonState");
        method.setAccessible(true);
        method.invoke(fragment);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}