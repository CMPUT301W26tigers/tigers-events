package com.example.eventsapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
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

import androidx.navigation.fragment.NavHostFragment;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class InboxIntentTest {
    private Users testUser;

    @Before
    public void setUp() {
        testUser = createInboxTestUser();
        UserManager.getInstance().setCurrentUser(testUser);
    }

    @Test
    public void openInbox_displaysLotteryOutcomeNotifications() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            prepareDemoInboxAndNavigateToAccount(scenario);
            onView(withId(R.id.btnTopIcon)).perform(click());

            onView(withText("Inbox")).check(matches(isDisplayed()));
            onView(withText("Event Invitation")).check(matches(isDisplayed()));
            onView(withText("Event waitlisted")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void inboxBack_returnsToAccountScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            prepareDemoInboxAndNavigateToAccount(scenario);
            onView(withId(R.id.btnTopIcon)).perform(click());
            onView(withId(R.id.btnInboxBack)).perform(click());

            onView(withId(R.id.btnTopIcon)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void acceptInvitation_removesInvitationFromInbox() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            prepareDemoInboxAndNavigateToAccount(scenario);
            onView(withId(R.id.btnTopIcon)).perform(click());
            onView(allOf(withText("Event Invitation"), isDisplayed())).perform(click());
            onView(allOf(withId(R.id.btnAcceptInvitation), isDisplayed())).perform(click());

            onView(withText("Event Invitation")).check(doesNotExist());
            onView(withText("Event waitlisted")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void declineInvitation_removesInvitationFromInbox() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            prepareDemoInboxAndNavigateToAccount(scenario);
            onView(withId(R.id.btnTopIcon)).perform(click());
            onView(allOf(withText("Event Invitation"), isDisplayed())).perform(click());
            onView(allOf(withId(R.id.btnDeclineInvitation), isDisplayed())).perform(click());

            onView(withText("Event Invitation")).check(doesNotExist());
            onView(withText("Event waitlisted")).check(matches(isDisplayed()));
        }
    }

    private void prepareDemoInboxAndNavigateToAccount(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(activity -> {
            NavHostFragment navHostFragment = (NavHostFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                navHostFragment.getNavController().navigate(R.id.accountFragment);
            }
        });
    }

    private Users createInboxTestUser() {
        Users user = new Users(
                "Inbox",
                "Tester",
                "inbox@test.com",
                "Edmonton, AB",
                "Entrant",
                true
        );

        LotteryNotificationController controller = new LotteryNotificationController();
        controller.notifyWaitlisted(user, "River Valley Night Run");
        controller.notifyChosenFromWaitlist(user, "Campus Startup Showcase");
        return user;
    }
}
