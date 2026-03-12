package com.example.eventsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements EventDialogFragment.EventDialogListener,
        DeleteEventDialogFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private Button addEventButton;
    private Button deleteEventButton;
    private FirebaseFirestore db;

    private CollectionReference eventsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addEventButton = findViewById(R.id.buttonAddEvent);
        deleteEventButton = findViewById(R.id.buttonDeleteEvent);

        addEventButton.setOnClickListener(view -> {
            EventDialogFragment eventDialogFragment = new EventDialogFragment();
            eventDialogFragment.show(getSupportFragmentManager(), "Add Event");
        });

        deleteEventButton.setOnClickListener(view -> {
            DeleteEventDialogFragment deleteEventDialogFragment = new DeleteEventDialogFragment();
            deleteEventDialogFragment.show(getSupportFragmentManager(), "Delete Event");
        });

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);
        handleDeepLink(getIntent());
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
                        snapshot.getReference().delete()
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete event " + trimmedEventName, e));
                        return;
                    }

                    eventsRef.whereEqualTo("name", trimmedEventName).get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    doc.getReference().delete();
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
