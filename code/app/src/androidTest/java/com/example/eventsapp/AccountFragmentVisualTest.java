package com.example.eventsapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.action.ViewActions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;

/**
 * Instrumented test to verify that the AccountFragment correctly displays
 * user information from the UserManager.
 */
@RunWith(AndroidJUnit4.class)
public class AccountFragmentVisualTest {
    private Users testUser;

    @Before
    public void setUp() {
        testUser = new Users("Visual", "Test", "visual@example.com", "password", "1234567890", "test-device");
        testUser.setId("visual-user-id");
        testUser.setLocation("Test City");
        testUser.setAccountType("Test Type");
        UserManager.getInstance().setCurrentUser(testUser);
    }

    @Test
    public void testAccountInfoMatchesUser() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.accountFragment)).perform(ViewActions.click());

            onView(withId(R.id.tvGreeting)).check(matches(withText(containsString("Visual"))));
            onView(withId(R.id.tvNameValue)).check(matches(withText("Visual Test")));
            onView(withId(R.id.tvEmailValue)).check(matches(withText("visual@example.com")));
            onView(withId(R.id.tvLocationValue)).check(matches(withEffectiveVisibility(GONE)));
            onView(withId(R.id.tvAccountTypeValue)).check(matches(withText("Test Type")));
        }
    }
}
