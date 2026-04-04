package com.example.eventsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements EventDialogFragment.EventDialogListener, DeleteEventDialogFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private CollectionReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");
        userRef = db.collection("users");

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Hide bottom nav on sign-in page
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.signInFragment
                    || id == R.id.eventDetailFragment
                    || id == R.id.createEventFragment
                    || id == R.id.editEventFragment) {
                bottomNav.setVisibility(View.GONE);
            } else {
                bottomNav.setVisibility(View.VISIBLE);
            }
        });

        handleDeepLink(getIntent());
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
        if (event == null) {
            Log.w(TAG, "updateEvent called with null event");
            return;
        }

        String previousName = event.getName();
        event.setName(title);
        event.setAmount(amount);

        String docId = (event.getId() != null && !event.getId().isEmpty()) ? event.getId() : event.getName();
        if (docId == null || docId.trim().isEmpty()) {
            Log.w(TAG, "updateEvent called with empty document id");
            return;
        }

        if (previousName != null && !previousName.equals(title)) {
            Log.d(TAG, "Event renamed from " + previousName + " to " + title);
        }

        saveEvent(docId, event);
    }

    @Override
    public void addEvent(Event event) {
        if (event == null) {
            Log.w(TAG, "addEvent called with null event");
            return;
        }

        String docId = (event.getId() != null && !event.getId().isEmpty()) ? event.getId() : event.getName();
        saveEvent(docId, event);
    }

    @Override
    public void onConfirmPressed(String eventName) {
        if (eventName == null || eventName.trim().isEmpty()) {
            Log.w(TAG, "onConfirmPressed called with empty event name");
            return;
        }

        String trimmedEventName = eventName.trim();
        eventsRef.document(trimmedEventName).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String eventId = snapshot.getId();
                        EventCleanupHelper.deleteEventCompletely(eventId,
                                () -> Log.d(TAG, "Deleted event: " + trimmedEventName),
                                e -> Log.e(TAG, "Failed to delete event " + trimmedEventName, e));
                        return;
                    }

                    eventsRef.whereEqualTo("name", trimmedEventName).get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    String eventId = doc.getString("id");
                                    if (eventId == null) eventId = doc.getId();
                                    EventCleanupHelper.deleteEventCompletely(eventId,
                                            () -> Log.d(TAG, "Deleted event: " + trimmedEventName),
                                            e -> Log.e(TAG, "Failed to delete event " + trimmedEventName, e));
                                    return;
                                }
                                Log.w(TAG, "No event found to delete: " + trimmedEventName);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to query event " + trimmedEventName, e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to look up event " + trimmedEventName, e));
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

    private void saveEvent(String documentId, Event event) {
        if (documentId == null || documentId.trim().isEmpty()) {
            Log.w(TAG, "Refused to save event with empty document ID");
            return;
        }

        DocumentReference docRef = eventsRef.document(documentId.trim());
        Map<String, Object> data = new HashMap<>();
        data.put("id", event.getId());
        data.put("name", event.getName());
        data.put("amount", event.getAmount());
        data.put("description", event.getDescription());
        data.put("posterUrl", event.getPosterUrl());
        data.put("sampleSize", event.getSampleSize());

        docRef.set(data)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save event " + documentId, e));
    }
}
