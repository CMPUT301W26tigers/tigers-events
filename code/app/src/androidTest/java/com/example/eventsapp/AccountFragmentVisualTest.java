package com.example.eventsapp;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        try (ActivityScenario<TestHostActivity> scenario = ActivityScenario.launch(TestHostActivity.class)) {
            scenario.onActivity(activity -> {
                AccountFragment fragment = new AccountFragment();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .commitNow();

                assertThat(activity.<android.widget.TextView>findViewById(R.id.tvGreeting)
                        .getText().toString(), containsString("Visual"));
                assertEquals("Visual Test", activity.<android.widget.TextView>findViewById(R.id.tvNameValue)
                        .getText().toString());
                assertEquals("visual@example.com", activity.<android.widget.TextView>findViewById(R.id.tvEmailValue)
                        .getText().toString());
                assertEquals(android.view.View.GONE, activity.findViewById(R.id.tvLocationValue).getVisibility());
                assertEquals("Test Type", activity.<android.widget.TextView>findViewById(R.id.tvAccountTypeValue)
                        .getText().toString());
            });
        }
    }
}
