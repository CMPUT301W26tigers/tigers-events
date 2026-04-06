package com.example.eventsapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

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
        try (ActivityScenario<TestHostActivity> scenario = ActivityScenario.launch(TestHostActivity.class)) {
            showInbox(scenario);
            scenario.onActivity(activity -> {
                InboxFragment inboxFragment = requireCurrentFragment(activity, InboxFragment.class);
                View root = inboxFragment.getView();
                assertNotNull(root);

                assertEquals("Inbox", ((TextView) root.findViewById(R.id.tvInboxTitle)).getText().toString());
                List<String> titles = getInboxTitles(root);
                assertTrue(titles.contains("Event Invitation"));
                assertTrue(titles.contains("Event waitlisted"));
            });
        }
    }

    @Test
    public void inboxBack_returnsToAccountScreen() {
        try (ActivityScenario<TestHostActivity> scenario = ActivityScenario.launch(TestHostActivity.class)) {
            showAccountThenInbox(scenario);
            scenario.onActivity(activity -> {
                InboxFragment inboxFragment = requireCurrentFragment(activity, InboxFragment.class);
                View root = inboxFragment.getView();
                assertNotNull(root);
                root.findViewById(R.id.btnInboxBack).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            scenario.onActivity(activity -> {
                AccountFragment accountFragment = requireCurrentFragment(activity, AccountFragment.class);
                View root = accountFragment.getView();
                assertNotNull(root);
                assertEquals(View.VISIBLE, root.findViewById(R.id.btnTopIcon).getVisibility());
            });
        }
    }

    @Test
    public void acceptInvitation_removesInvitationFromInbox() {
        try (ActivityScenario<TestHostActivity> scenario = ActivityScenario.launch(TestHostActivity.class)) {
            showInbox(scenario);
            scenario.onActivity(activity -> {
                InboxFragment inboxFragment = requireCurrentFragment(activity, InboxFragment.class);
                View root = inboxFragment.getView();
                assertNotNull(root);

                View invitationCard = findNotificationCard(root, "Event Invitation");
                assertNotNull(invitationCard);
                invitationCard.findViewById(R.id.notificationHeader).performClick();
                invitationCard.findViewById(R.id.btnAcceptInvitation).performClick();

                List<String> titles = getInboxTitles(root);
                assertFalse(titles.contains("Event Invitation"));
                assertTrue(titles.contains("Event waitlisted"));
            });
        }
    }

    @Test
    public void declineInvitation_removesInvitationFromInbox() {
        try (ActivityScenario<TestHostActivity> scenario = ActivityScenario.launch(TestHostActivity.class)) {
            showInbox(scenario);
            scenario.onActivity(activity -> {
                InboxFragment inboxFragment = requireCurrentFragment(activity, InboxFragment.class);
                View root = inboxFragment.getView();
                assertNotNull(root);

                View invitationCard = findNotificationCard(root, "Event Invitation");
                assertNotNull(invitationCard);
                invitationCard.findViewById(R.id.notificationHeader).performClick();
                invitationCard.findViewById(R.id.btnDeclineInvitation).performClick();

                List<String> titles = getInboxTitles(root);
                assertFalse(titles.contains("Event Invitation"));
                assertTrue(titles.contains("Event waitlisted"));
            });
        }
    }

    private void showInbox(ActivityScenario<TestHostActivity> scenario) {
        scenario.onActivity(activity -> {
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, newTestInboxFragment())
                    .commitNow();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void showAccountThenInbox(ActivityScenario<TestHostActivity> scenario) {
        scenario.onActivity(activity -> {
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new AccountFragment())
                    .commitNow();

            activity.getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, newTestInboxFragment())
                    .addToBackStack(null)
                    .commit();
            activity.getSupportFragmentManager().executePendingTransactions();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private Fragment getCurrentFragment(TestHostActivity activity) {
        Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(android.R.id.content);
        assertNotNull(currentFragment);
        return currentFragment;
    }

    private <T extends Fragment> T requireCurrentFragment(TestHostActivity activity, Class<T> expectedType) {
        Fragment fragment = getCurrentFragment(activity);
        assertTrue(expectedType.isInstance(fragment));
        return expectedType.cast(fragment);
    }

    private List<String> getInboxTitles(View inboxRoot) {
        List<String> titles = new ArrayList<>();
        LinearLayout container = inboxRoot.findViewById(R.id.notificationContainer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            TextView notificationTitle = child.findViewById(R.id.tvNotificationTitle);
            if (notificationTitle != null) {
                titles.add(notificationTitle.getText().toString());
                continue;
            }

            TextView waitlistTitle = child.findViewById(R.id.tvWaitlistGroupTitle);
            if (waitlistTitle != null) {
                titles.add(waitlistTitle.getText().toString());
            }
        }
        return titles;
    }

    private View findNotificationCard(View inboxRoot, String title) {
        LinearLayout container = inboxRoot.findViewById(R.id.notificationContainer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            TextView notificationTitle = child.findViewById(R.id.tvNotificationTitle);
            if (notificationTitle != null && title.equals(notificationTitle.getText().toString())) {
                return child;
            }
        }
        return null;
    }

    private InboxFragment newTestInboxFragment() {
        return new TestInboxFragment();
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

    public static class TestInboxFragment extends InboxFragment {
        @Override
        public void onViewCreated(View view, android.os.Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            view.findViewById(R.id.btnInboxBack)
                    .setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
    }
}
