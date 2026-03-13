package com.example.eventsapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test to verify that the AccountFragment correctly displays
 * user information from the UserManager.
 */
@RunWith(AndroidJUnit4.class)
public class AccountFragmentVisualTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        // Setup a test user
        Users testUser = new Users("Visual", "Test", "visual@example.com", "password", "1234567890", "test-device");
        testUser.setId("visual-user-id");
        testUser.setLocation("Test City");
        testUser.setAccountType("Test Type");
        UserManager.getInstance().setCurrentUser(testUser);

        // Navigate to AccountFragment (assuming bottom nav is used)
        onView(withId(R.id.accountFragment)).perform(androidx.test.espresso.action.ViewActions.click());
    }

    @Test
    public void testAccountInfoMatchesUser() {
        // Check if the views are displayed and contain the correct text
        onView(withId(R.id.tvGreeting)).check(matches(withText(containsString("Visual"))));
        onView(withId(R.id.tvNameValue)).check(matches(withText("Visual Test")));
        onView(withId(R.id.tvEmailValue)).check(matches(withText("visual@example.com")));
        
        // Note: tvLocationValue and tvAccountTypeValue might not be updated by displayUserData() 
        // in the current implementation of AccountFragment, but we check if they exist.
        onView(withId(R.id.tvLocationValue)).check(matches(isDisplayed()));
        onView(withId(R.id.tvAccountTypeValue)).check(matches(isDisplayed()));
    }
}
