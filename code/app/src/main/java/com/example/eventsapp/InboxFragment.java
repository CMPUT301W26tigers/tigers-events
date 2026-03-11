package com.example.eventsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class InboxFragment extends Fragment {
    public InboxFragment() {
        super(R.layout.fragment_inbox);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton backButton = view.findViewById(R.id.btnInboxBack);
        backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        renderNotifications(view.findViewById(R.id.notificationContainer), view.findViewById(R.id.tvEmptyState));
    }

    private void renderNotifications(LinearLayout container, TextView emptyState) {
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (UserNotification notification : UserSession.getCurrentUser().getNotifications()) {
            View itemView = inflater.inflate(R.layout.item_inbox_notification, container, false);
            bindNotification(itemView, notification);
            container.addView(itemView);
        }

        emptyState.setVisibility(container.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void bindNotification(View itemView, UserNotification notification) {
        View header = itemView.findViewById(R.id.notificationHeader);
        LinearLayout details = itemView.findViewById(R.id.notificationDetails);
        ImageView icon = itemView.findViewById(R.id.ivNotificationIcon);
        ImageView chevron = itemView.findViewById(R.id.ivNotificationChevron);
        TextView title = itemView.findViewById(R.id.tvNotificationTitle);
        TextView eventName = itemView.findViewById(R.id.tvNotificationEvent);
        TextView message = itemView.findViewById(R.id.tvNotificationMessage);

        title.setText(notification.getTitle());
        eventName.setText(notification.getEventName());
        message.setText(notification.getMessage());

        header.setBackgroundColor(ContextCompat.getColor(requireContext(), getBackgroundColor(notification.getType())));
        icon.setImageResource(getIcon(notification.getType()));

        View.OnClickListener toggleListener = v -> {
            boolean isExpanded = details.getVisibility() == View.VISIBLE;
            details.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            chevron.setRotation(isExpanded ? 0f : 180f);
        };

        header.setOnClickListener(toggleListener);
        chevron.setOnClickListener(toggleListener);
    }

    @ColorRes
    private int getBackgroundColor(UserNotification.Type type) {
        switch (type) {
            case INVITATION:
                return R.color.notification_green;
            case NOT_SELECTED:
                return R.color.notification_red;
            case WAITLISTED:
            default:
                return R.color.notification_grey;
        }
    }

    @DrawableRes
    private int getIcon(UserNotification.Type type) {
        switch (type) {
            case INVITATION:
                return android.R.drawable.checkbox_on_background;
            case NOT_SELECTED:
                return android.R.drawable.ic_delete;
            case WAITLISTED:
            default:
                return android.R.drawable.ic_menu_recent_history;
        }
    }
}
