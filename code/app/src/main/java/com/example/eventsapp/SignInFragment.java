package com.example.eventsapp;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A fragment that handles user authentication, including sign-in and sign-up.
 * It also supports automatic login if a device ID is recognized and remembered.
 */
public class SignInFragment extends Fragment {
    private boolean isSignUpMode = false;

    /**
     * Default constructor for SignInFragment.
     * Uses the layout R.layout.fragment_sign_in.
     */
    public SignInFragment() {
        super(R.layout.fragment_sign_in);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned. Sets up auto-login logic, UI component references, and listeners for
     * mode toggling and authentication actions.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Try auto-login via remembered device
        String deviceId = Settings.Secure.getString(
                requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (deviceId != null && !deviceId.isEmpty()) {
            db.collection("users").whereEqualTo("deviceId", deviceId).get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Users user = querySnapshot.getDocuments().get(0).toObject(Users.class);
                            if (user != null) {
                                user.setId(querySnapshot.getDocuments().get(0).getId());
                                UserManager.getInstance().setCurrentUser(user);
                                TigerToast.show(getContext(), "Welcome back, " + user.getName(), Toast.LENGTH_SHORT);
                                navigateToExplore(view);
                            }
                        }
                    });
        }

        // Find views
        TextInputEditText etUsername = view.findViewById(R.id.etUsername);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);
        TextInputEditText etFirstName = view.findViewById(R.id.etFirstName);
        TextInputEditText etLastName = view.findViewById(R.id.etLastName);
        TextInputEditText etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        TextInputEditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        MaterialButton btnSignIn = view.findViewById(R.id.btnSignIn);
        CheckBox cbRememberDevice = view.findViewById(R.id.cbRememberDevice);
        TextView tvToggleMode = view.findViewById(R.id.tvToggleMode);

        // Sign-up only fields
        TextView tvFirstName = view.findViewById(R.id.tvFirstName);
        TextInputLayout tilFirstName = view.findViewById(R.id.tilFirstName);
        TextView tvLastName = view.findViewById(R.id.tvLastName);
        TextInputLayout tilLastName = view.findViewById(R.id.tilLastName);
        TextView tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber);
        TextInputLayout tilPhoneNumber = view.findViewById(R.id.tilPhoneNumber);
        TextView tvConfirmPassword = view.findViewById(R.id.tvConfirmPassword);
        TextInputLayout tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);

        View[] signUpViews = {
                tvFirstName, tilFirstName, tvLastName, tilLastName,
                tvPhoneNumber, tilPhoneNumber, tvConfirmPassword, tilConfirmPassword,
                cbRememberDevice
        };

        // Toggle between sign-in and sign-up
        tvToggleMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            int visibility = isSignUpMode ? View.VISIBLE : View.GONE;
            for (View signUpView : signUpViews) {
                signUpView.setVisibility(visibility);
            }
            btnSignIn.setText(isSignUpMode ? "Sign Up" : "Sign In");
            tvToggleMode.setText(isSignUpMode
                    ? "Already have an account? Sign In"
                    : "Don't have an account? Sign Up");
        });

        // Handle sign-in or sign-up
        btnSignIn.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                TigerToast.show(getContext(), "Please enter email and password", Toast.LENGTH_SHORT);
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                TigerToast.show(getContext(), "Please enter a valid email", Toast.LENGTH_SHORT);
                return;
            }

            if (isSignUpMode) {
                handleSignUp(view, db, etFirstName, etLastName, etPhoneNumber,
                        etConfirmPassword, cbRememberDevice, email, password, deviceId);
            } else {
                handleSignIn(view, db, email, password);
            }
        });
    }

    /**
     * Processes a sign-in request by verifying credentials against Firestore.
     *
     * @param view The current view.
     * @param db The Firestore database instance.
     * @param email The user's email.
     * @param password The user's password.
     */
    private void handleSignIn(View view, FirebaseFirestore db, String email, String password) {
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        Users user = documentSnapshot.toObject(Users.class);
                        if (user != null) {
                            user.setId(documentSnapshot.getId());
                            if (user.login(email, password)) {
                                UserManager.getInstance().setCurrentUser(user);
                                TigerToast.show(getContext(), "Welcome " + user.getName(), Toast.LENGTH_SHORT);
                                navigateToExplore(view);
                            } else {
                                TigerToast.show(getContext(), "Invalid password", Toast.LENGTH_SHORT);
                            }
                        }
                    } else {
                        TigerToast.show(getContext(), "User not found", Toast.LENGTH_SHORT);
                    }
                })
                .addOnFailureListener(e ->
                        TigerToast.show(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT));
    }

    /**
     * Processes a sign-up request by creating a new user document in Firestore.
     *
     * @param view The current view.
     * @param db The Firestore database instance.
     * @param etFirstName Input field for first name.
     * @param etLastName Input field for last name.
     * @param etPhoneNumber Input field for phone number.
     * @param etConfirmPassword Input field for password confirmation.
     * @param cbRememberDevice Checkbox for remembering the device.
     * @param email The user's email.
     * @param password The user's password.
     * @param deviceId The unique ID of the device.
     */
    private void handleSignUp(View view, FirebaseFirestore db,
                              TextInputEditText etFirstName, TextInputEditText etLastName,
                              TextInputEditText etPhoneNumber, TextInputEditText etConfirmPassword,
                              CheckBox cbRememberDevice, String email, String password, String deviceId) {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
            TigerToast.show(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT);
            return;
        }

        if (!password.equals(confirmPassword)) {
            TigerToast.show(getContext(), "Passwords do not match", Toast.LENGTH_SHORT);
            return;
        }

        // Check for duplicate email
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        TigerToast.show(getContext(), "An account with this email already exists", Toast.LENGTH_SHORT);
                        return;
                    }

                    String userDeviceId = cbRememberDevice.isChecked() ? deviceId : null;
                    Users newUser = new Users(firstName, lastName, email, password, phone, userDeviceId);

                    DocumentReference newDocRef = db.collection("users").document();
                    newUser.setId(newDocRef.getId());
                    newDocRef.set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                UserManager.getInstance().setCurrentUser(newUser);
                                TigerToast.show(getContext(), "Account created! Welcome " + newUser.getName(), Toast.LENGTH_SHORT);
                                navigateToExplore(view);
                            })
                            .addOnFailureListener(e ->
                                    TigerToast.show(getContext(), "Error creating account: " + e.getMessage(), Toast.LENGTH_SHORT));
                })
                .addOnFailureListener(e ->
                        TigerToast.show(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT));
    }

    /**
     * Navigates to the explore events screen and removes the sign-in fragment from the back stack.
     *
     * @param view The current view.
     */
    private void navigateToExplore(View view) {
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.signInFragment, true)
                .build();
        Navigation.findNavController(view).navigate(R.id.exploreFragment, null, navOptions);
    }
}
