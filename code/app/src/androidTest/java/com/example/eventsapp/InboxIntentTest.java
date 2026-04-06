package com.example.eventsapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import com.example.eventsapp.MainActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class InboxIntentTest {
    @Test
    public void openInbox_displaysLotteryOutcomeNotifications() {
        UserSession.resetDemoUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnNotifications)).perform(click());

            onView(withText("Inbox")).check(matches(isDisplayed()));
            onView(withText("Event Invitation")).check(matches(isDisplayed()));
            onView(withText("Event waitlisted")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void inboxBack_returnsToAccountScreen() {
        UserSession.resetDemoUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnNotifications)).perform(click());
            onView(withId(R.id.btnInboxBack)).perform(click());

            onView(withId(R.id.btnNotifications)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void acceptInvitation_removesInvitationFromInbox() {
        UserSession.resetDemoUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnNotifications)).perform(click());
            onView(allOf(withText("Event Invitation"), isDisplayed())).perform(click());
            onView(allOf(withId(R.id.btnAcceptInvitation), isDisplayed())).perform(click());

            onView(withText("Event Invitation")).check(doesNotExist());
            onView(withText("Event waitlisted")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void declineInvitation_removesInvitationFromInbox() {
        UserSession.resetDemoUser();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnNotifications)).perform(click());
            onView(allOf(withText("Event Invitation"), isDisplayed())).perform(click());
            onView(allOf(withId(R.id.btnDeclineInvitation), isDisplayed())).perform(click());

            onView(withText("Event Invitation")).check(doesNotExist());
            onView(withText("Event waitlisted")).check(matches(isDisplayed()));
        }
    }
}