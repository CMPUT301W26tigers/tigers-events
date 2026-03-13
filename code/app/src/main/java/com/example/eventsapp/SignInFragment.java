package com.example.eventsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInFragment extends Fragment {
    public SignInFragment() {
        super(R.layout.fragment_sign_in);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText etUsername = view.findViewById(R.id.etUsername);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);
        MaterialButton btnSignIn = view.findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").whereEqualTo("email", email).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Assuming email is unique, get the first match
                            var documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            Users user = documentSnapshot.toObject(Users.class);
                            if (user != null) {
                                user.setId(documentSnapshot.getId());
                                if (user.login(email, password)) {
                                    // Successful login
                                    UserManager.getInstance().setCurrentUser(user);
                                    Toast.makeText(getContext(), "Welcome " + user.getName(), Toast.LENGTH_SHORT).show();
                                    // Navigate to the main app page
                                    Navigation.findNavController(view).navigate(R.id.exploreFragment);
                                } else {
                                    Toast.makeText(getContext(), "Invalid password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
