package com.example.eventsapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * A fragment that allows the user to view and manage their account information.
 * Users can update their name, email, and device tracking preferences.
 * They can also sign out from this fragment.
 */
public class AccountFragment extends Fragment {

    private ImageButton editNameButton;
    private ImageButton editEmailButton;
    private ImageButton inboxIconButton;
    private TextView deleteAccountButton;
    private MaterialCheckBox notificationsCheckbox;
    private MaterialButton notificationsButton;

    private TextView tvGreeting;
    private TextView tvNameValue;
    private TextView tvEmailValue;
    private TextView tvLocationValue;
    private TextView tvAccountTypeValue;

    private FirebaseFirestore db;

    /**
     * Default constructor for AccountFragment.
     * Uses the layout R.layout.fragment_account.
     */
    public AccountFragment() {
        super(R.layout.fragment_account);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * Initializes the UI components and sets up click listeners for editing account details.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        editNameButton = view.findViewById(R.id.btnEditName);
        editEmailButton = view.findViewById(R.id.btnEditEmail);
        inboxIconButton = view.findViewById(R.id.btnTopIcon);
        deleteAccountButton = view.findViewById(R.id.btnDeleteAccount);
        notificationsCheckbox = view.findViewById(R.id.cbNotifications);
        notificationsButton = view.findViewById(R.id.btnNotifications);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvNameValue = view.findViewById(R.id.tvNameValue);
        tvEmailValue = view.findViewById(R.id.tvEmailValue);
        tvLocationValue = view.findViewById(R.id.tvLocationValue);
        tvAccountTypeValue = view.findViewById(R.id.tvAccountTypeValue);

        // Populate user data
        displayUserData();

        // Set listeners
        if (editNameButton != null) {
            editNameButton.setOnClickListener(v -> showEditDialog("name", "Enter new name", InputType.TYPE_CLASS_TEXT));
        }

        if (editEmailButton != null) {
            editEmailButton.setOnClickListener(v -> showEditDialog("email", "Enter new email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
        }

        if (deleteAccountButton != null) {
            deleteAccountButton.setOnClickListener(v -> {
                // Handle delete account button click
            });
        }

        if (inboxIconButton != null) {
            inboxIconButton.setOnClickListener(v -> openInbox(v));
        }

        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> openInbox(v));
        }

        // Remember device checkbox
        MaterialCheckBox cbRememberDevice = view.findViewById(R.id.cbRememberDevice);
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        Users currentUser = UserManager.getInstance().getCurrentUser();

        if (cbRememberDevice != null && currentUser != null) {
            // Set initial state based on whether deviceId is already saved
            cbRememberDevice.setChecked(
                    currentUser.getDeviceId() != null && currentUser.getDeviceId().equals(deviceId));

            cbRememberDevice.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String newDeviceId = isChecked ? deviceId : null;
                currentUser.setDeviceId(newDeviceId);
                updateUserField("deviceId", newDeviceId);
            });
        }

        if (notificationsCheckbox != null && currentUser != null) {
            notificationsCheckbox.setChecked(currentUser.isNotificationsEnabled());
            notificationsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentUser.setNotificationsEnabled(isChecked);
                updateUserField("notificationsEnabled", isChecked);
            });
        }

        // Sign out button
        MaterialButton btnSignOut = view.findViewById(R.id.btnSignOut);
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                Users user = UserManager.getInstance().getCurrentUser();
                if (user != null && user.getId() != null && user.getDeviceId() != null) {
                    // Clear deviceId in Firestore to prevent auto-login
                    user.setDeviceId(null);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("deviceId", null);
                    db.collection("users").document(user.getId())
                            .set(updates, SetOptions.merge());
                }
                UserManager.getInstance().setCurrentUser(null);
                Navigation.findNavController(view).navigate(R.id.signInFragment);
            });
        }
    }

    /**
     * Updates the UI text views with the current user's information from the {@link UserManager}.
     */
    private void displayUserData() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (tvGreeting != null) {
                String firstName = currentUser.getFirstName();
                tvGreeting.setText("Hi, " + (firstName != null && !firstName.isEmpty() ? firstName : currentUser.getName()));
            }
            if (tvNameValue != null) {
                tvNameValue.setText(currentUser.getName());
            }
            if (tvEmailValue != null) {
                tvEmailValue.setText(currentUser.getEmail());
            }
            if (tvLocationValue != null) {
                tvLocationValue.setText(currentUser.getLocation() != null ? currentUser.getLocation() : "Not set");
            }
            if (tvAccountTypeValue != null) {
                tvAccountTypeValue.setText(currentUser.getAccountType() != null ? currentUser.getAccountType() : "Not set");
            }
            if (notificationsCheckbox != null) {
                notificationsCheckbox.setChecked(currentUser.isNotificationsEnabled());
            }
        } else {
            if (tvGreeting != null) {
                tvGreeting.setText("Hi, Guest");
            }
        }
    }

    /**
     * Displays an {@link AlertDialog} allowing the user to edit a specific field of their profile.
     *
     * @param field The name of the field being edited (e.g., "name", "email").
     * @param title The title to display on the dialog.
     * @param inputType The input type for the EditText in the dialog.
     */
    private void showEditDialog(String field, String title, int inputType) {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);

        final EditText input = new EditText(getContext());
        input.setInputType(inputType);
        // Pre-fill with current value
        if (field.equals("name")) {
            input.setText(currentUser.getName());
        } else if (field.equals("email")) {
            input.setText(currentUser.getEmail());
        }
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                updateUserField(field, newValue);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Updates a specific user field in the Firestore database and the local {@link Users} object.
     *
     * @param field The name of the field to update in Firestore.
     * @param newValue The new value for the field.
     */
    private void updateUserField(String field, Object newValue) {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(getContext(), "Error: User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(field, newValue);

        db.collection("users").document(currentUser.getId())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Update local user object
                    if (field.equals("name")) {
                        currentUser.setName((String) newValue);
                    } else if (field.equals("email")) {
                        currentUser.setEmail((String) newValue);
                    } else if (field.equals("deviceId")) {
                        currentUser.setDeviceId((String) newValue);
                    } else if (field.equals("notificationsEnabled") && newValue instanceof Boolean) {
                        currentUser.setNotificationsEnabled((Boolean) newValue);
                    }
                    displayUserData();
                    Toast.makeText(getContext(), field + " updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating " + field, e);
                    Toast.makeText(getContext(), "Failed to update " + field, Toast.LENGTH_SHORT).show();
                });
    }

    private void openInbox(View view) {
        Navigation.findNavController(view).navigate(R.id.action_accountFragment_to_inboxFragment);
    }
}
