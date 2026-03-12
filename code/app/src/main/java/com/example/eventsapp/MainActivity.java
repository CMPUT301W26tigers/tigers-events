package com.example.eventsapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
        implements EventDialogFragment.EventDialogListener,
        DeleteEventDialogFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    private CollectionReference eventsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        eventsRef = FirebaseFirestore.getInstance().collection("events");

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);
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

        if (previousName == null || Objects.equals(previousName, title)) {
            saveEvent(title, event);
            return;
        }

        eventsRef.document(previousName).delete()
                .addOnSuccessListener(unused -> saveEvent(title, event))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to rename event " + previousName, e));
    }

    @Override
    public void addEvent(Event event) {
        if (event == null) {
            Log.w(TAG, "addEvent called with null event");
            return;
        }

        saveEvent(event.getName(), event);
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

    private void saveEvent(String documentId, Event event) {
        if (documentId == null || documentId.trim().isEmpty()) {
            Log.w(TAG, "Refused to save event with empty document ID");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", event.getName());
        data.put("amount", event.getAmount());

        eventsRef.document(documentId.trim()).set(data)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save event " + documentId, e));
    }
}
