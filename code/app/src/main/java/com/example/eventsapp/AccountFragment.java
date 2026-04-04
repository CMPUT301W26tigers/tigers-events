package com.example.eventsapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
    private MaterialButton managementToolsButton;

    private TextView tvGreeting;
    private TextView tvNameValue;
    private TextView tvEmailValue;
    private TextView tvLocationValue;
    private TextView tvAccountTypeValue;

    private FrameLayout avatarContainer;
    private ImageView ivProfilePicture;
    private TextView tvAvatarInitial;
    private ProgressBar progressAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;

    private FirebaseFirestore db;

    /**
     * Default constructor for AccountFragment.
     * Uses the layout R.layout.fragment_account.
     */
    public AccountFragment() {
        super(R.layout.fragment_account);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserData();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) uploadProfilePicture(uri);
                });
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
        managementToolsButton = view.findViewById(R.id.btnManagementTools);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvNameValue = view.findViewById(R.id.tvNameValue);
        tvEmailValue = view.findViewById(R.id.tvEmailValue);
        tvLocationValue = view.findViewById(R.id.tvLocationValue);
        tvAccountTypeValue = view.findViewById(R.id.tvAccountTypeValue);

        avatarContainer = view.findViewById(R.id.avatarContainer);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial);
        progressAvatar = view.findViewById(R.id.progressAvatar);

        avatarContainer.setOnClickListener(v -> showAvatarOptions());

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
            deleteAccountButton.setOnClickListener(v -> showDeleteConfirmation());
        }

        ImageButton btnDeleteIcon = view.findViewById(R.id.btnDeleteIcon);
        if (btnDeleteIcon != null) {
            btnDeleteIcon.setOnClickListener(v -> showDeleteConfirmation());
        }

        if (inboxIconButton != null) {
            inboxIconButton.setOnClickListener(v -> openInbox(v));
        }

        if (managementToolsButton != null) {
            Users user = UserManager.getInstance().getCurrentUser();
            if (user != null && "Admin".equalsIgnoreCase(user.getAccountType())) {
                managementToolsButton.setVisibility(View.VISIBLE);
            }
            managementToolsButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AdminActivity.class);
                startActivity(intent);
            });
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

            // Avatar
            if (ivProfilePicture != null && tvAvatarInitial != null) {
                String url = currentUser.getProfilePictureUrl();
                if (url != null && !url.isEmpty()) {
                    ivProfilePicture.setVisibility(View.VISIBLE);
                    tvAvatarInitial.setVisibility(View.GONE);
                    Glide.with(this).load(url).circleCrop().into(ivProfilePicture);
                } else {
                    ivProfilePicture.setVisibility(View.GONE);
                    tvAvatarInitial.setVisibility(View.VISIBLE);
                    String name = currentUser.getName();
                    tvAvatarInitial.setText(name != null && !name.isEmpty()
                            ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
                }
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
            TigerToast.show(getContext(), "Error: User ID not found", Toast.LENGTH_SHORT);
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
                    TigerToast.show(getContext(), field + " updated", Toast.LENGTH_SHORT);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating " + field, e);
                    TigerToast.show(getContext(), "Failed to update " + field, Toast.LENGTH_SHORT);
                });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This will also delete all events you created. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) return;

        String userId = currentUser.getId();

        db.collection("events")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        db.collection("events").document(doc.getId()).delete();
                    }
                    db.collection("users").document(userId).delete()
                            .addOnSuccessListener(aVoid -> {
                                UserManager.getInstance().setCurrentUser(null);
                                Navigation.findNavController(requireView()).navigate(R.id.signInFragment);
                            });
                });
    }

    private void showAvatarOptions() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String url = currentUser.getProfilePictureUrl();
        boolean hasPhoto = url != null && !url.isEmpty();

        String[] options = hasPhoto
                ? new String[]{"Change Photo", "Remove Photo", "Cancel"}
                : new String[]{"Upload Photo", "Cancel"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (hasPhoto) {
                        if (which == 0) pickImageLauncher.launch("image/*");
                        else if (which == 1) removeProfilePicture();
                    } else {
                        if (which == 0) pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void uploadProfilePicture(Uri uri) {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) return;

        progressAvatar.setVisibility(View.VISIBLE);
        avatarContainer.setEnabled(false);

        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("profile_pictures/" + currentUser.getId() + ".jpg");

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            String downloadUrl = downloadUri.toString();
                            db.collection("users").document(currentUser.getId())
                                    .update("profilePictureUrl", downloadUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        currentUser.setProfilePictureUrl(downloadUrl);
                                        progressAvatar.setVisibility(View.GONE);
                                        avatarContainer.setEnabled(true);
                                        if (isAdded()) {
                                            displayUserData();
                                            Toast.makeText(requireContext(), "Photo updated", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("AccountFragment", "Firestore update failed", e);
                                        progressAvatar.setVisibility(View.GONE);
                                        avatarContainer.setEnabled(true);
                                        if (isAdded()) Toast.makeText(requireContext(), "Failed to save photo", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("AccountFragment", "getDownloadUrl failed", e);
                            progressAvatar.setVisibility(View.GONE);
                            avatarContainer.setEnabled(true);
                            if (isAdded()) Toast.makeText(requireContext(), "Failed to get photo URL", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    Log.e("AccountFragment", "Storage putFile failed", e);
                    progressAvatar.setVisibility(View.GONE);
                    avatarContainer.setEnabled(true);
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeProfilePicture() {
        Users currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) return;

        progressAvatar.setVisibility(View.VISIBLE);
        avatarContainer.setEnabled(false);

        db.collection("users").document(currentUser.getId())
                .update("profilePictureUrl", "")
                .addOnSuccessListener(aVoid -> {
                    currentUser.setProfilePictureUrl("");
                    progressAvatar.setVisibility(View.GONE);
                    avatarContainer.setEnabled(true);
                    if (isAdded()) {
                        displayUserData();
                        Toast.makeText(requireContext(), "Photo removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressAvatar.setVisibility(View.GONE);
                    avatarContainer.setEnabled(true);
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to remove photo", Toast.LENGTH_SHORT).show();
                });
    }

    private void openInbox(View view) {
        Navigation.findNavController(view).navigate(R.id.action_accountFragment_to_inboxFragment);
    }
}
