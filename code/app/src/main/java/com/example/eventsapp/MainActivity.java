package com.example.eventsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements EventDialogFragment.EventDialogListener, DeleteEventDialogFragment.OnFragmentInteractionListener {

    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private CollectionReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        userRef = db.collection("users");

        // Add dummy users
        addDummyUsers();

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
            NavigationUI.setupWithNavController(bottomNav, navHost.getNavController());
        }
        handleDeepLink(getIntent());
    }

    private void addDummyUsers() {
        Users[] dummyUsers = {
                new Users("John Doe", "john@example.com", "password123", 1234567890),
                new Users("Jane Smith", "jane@example.com", "securePass", 1987654321),
                new Users("Tiger Wood", "tiger@tigers.com", "roaring", 1122334455),
        };

        for (Users user : dummyUsers) {
            addUser(user);
        }
    }

    public void addUser(Users user) {
        // Query to see if user already exists by email
        userRef.whereEqualTo("email", user.getEmail()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Create a new document with auto-generated ID
                        DocumentReference newDocRef = userRef.document();
                        user.setId(newDocRef.getId()); // Set the ID in the object before saving
                        newDocRef.set(user)
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "User added: " + user.getName()))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error adding user", e));
                    } else {
                        Log.d("Firestore", "User with email " + user.getEmail() + " already exists.");
                    }
                });
    }

    @Override
    public void updateEvent(Event event, String title, int amount) {
        event.setName(title);
        event.setAmount(amount);

        String docId = (event.getId() != null && !event.getId().isEmpty()) ? event.getId() : event.getName();
        DocumentReference docRef = eventsRef.document(docId);
        docRef.update("name", title, "amount", amount)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "DocumentSnapshot successfully updated!"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating event", e));
    }

    @Override
    public void addEvent(Event event) {
        String docId = (event.getId() != null && !event.getId().isEmpty()) ? event.getId() : event.getName();
        DocumentReference docRef = eventsRef.document(docId);
        Map<String, Object> data = new HashMap<>();
        data.put("id", event.getId());
        data.put("name", event.getName());
        data.put("amount", event.getAmount());
        data.put("description", event.getDescription());
        data.put("posterUrl", event.getPosterUrl());
        data.put("sampleSize", event.getSampleSize());
        docRef.set(data)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error writing event", e));
    }

    @Override
    public void onConfirmPressed(String eventName) {
        eventsRef.document(eventName).delete()
                .addOnFailureListener(e -> {
                    eventsRef.whereEqualTo("name", eventName).get()
                            .addOnSuccessListener(q -> {
                                for (QueryDocumentSnapshot doc : q) {
                                    doc.getReference().delete();
                                    break;
                                }
                            });
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri data = intent.getData();
        if (data == null || !"tigers-events".equals(data.getScheme()) || !"event".equals(data.getHost())) return;
        String path = data.getPath();
        if (path == null || !path.startsWith("/")) return;
        String eventId = path.substring(1).trim();
        if (eventId.isEmpty()) return;

        findViewById(android.R.id.content).post(() -> {
            NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHost != null) {
                Bundle args = new Bundle();
                args.putString("eventId", eventId);
                navHost.getNavController().navigate(R.id.eventDetailFragment, args);
            }
        });
    }
}
