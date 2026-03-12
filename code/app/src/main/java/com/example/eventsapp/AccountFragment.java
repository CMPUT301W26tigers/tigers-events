package com.example.eventsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

public class AccountFragment extends Fragment {
    public AccountFragment() {
        super(R.layout.fragment_account);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Users currentUser = UserSession.getCurrentUser();
        bindUser(view, currentUser);

        MaterialButton notificationsButton = view.findViewById(R.id.btnNotifications);
        ImageButton topIconButton = view.findViewById(R.id.btnTopIcon);
        MaterialCheckBox notificationsCheckBox = view.findViewById(R.id.cbNotifications);

        notificationsCheckBox.setChecked(currentUser.isNotificationsEnabled());
        notificationsCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> currentUser.setNotificationsEnabled(isChecked)
        );

        View.OnClickListener openInboxListener = v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_accountFragment_to_inboxFragment);
        };

        notificationsButton.setOnClickListener(openInboxListener);
        topIconButton.setOnClickListener(openInboxListener);
    }

    private void bindUser(View view, Users currentUser) {
        TextView greeting = view.findViewById(R.id.tvGreeting);
        TextView name = view.findViewById(R.id.tvNameValue);
        TextView email = view.findViewById(R.id.tvEmailValue);
        TextView location = view.findViewById(R.id.tvLocationValue);
        TextView accountType = view.findViewById(R.id.tvAccountTypeValue);

        greeting.setText(getString(R.string.greeting_format, currentUser.getFirstName()));
        name.setText(currentUser.getFullName());
        email.setText(currentUser.getEmail());
        location.setText(currentUser.getLocation());
        accountType.setText(currentUser.getAccountType());
    }
}
