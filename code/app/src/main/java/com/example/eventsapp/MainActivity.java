package com.example.eventsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity implements EventDialogFragment.EventDialogListener, DeleteEventDialogFragment.OnFragmentInteractionListener {

    private Button addEventButton;
    private Button deleteEventButton;

    private FirebaseFirestore db;

    private CollectionReference eventsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addEventButton = findViewById(R.id.buttonAddEvent);
        deleteEventButton = findViewById(R.id.buttonDeleteEvent);

        // Set listeners
        addEventButton.setOnClickListener(view -> {
            EventDialogFragment eventDialogFragment = new EventDialogFragment();
            eventDialogFragment.show(getSupportFragmentManager(),"Add Event");
        });

        deleteEventButton.setOnClickListener(view -> {
            DeleteEventDialogFragment deleteEventDialogFragment = new DeleteEventDialogFragment();
            deleteEventDialogFragment.show(getSupportFragmentManager(), "Delete Event");
        });

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
            NavigationUI.setupWithNavController(bottomNav, navHost.getNavController());
        }
        handleDeepLink(getIntent());

    }

    @Override
    public void updateEvent(Event event, String title, int amount) {
        event.setName(title);
        event.setAmount(amount);

        String docId = (event.getId() != null && !event.getId().isEmpty()) ? event.getId() : event.getName();
        DocumentReference docRef = eventsRef.document(docId);
        docRef.update("name", title,
                        "amount", amount)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore", "DocumentSnapshot successfully written!");
                    }
                });
        // Updating the database using delete + addition
    }

    @Override
    public void addEvent(Event event){

        String docId = (event.getId() != null && !event.getId().isEmpty()) ? event.getId() : event.getName();
        DocumentReference docRef = eventsRef.document(docId);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", event.getId());
        data.put("name", event.getName());
        data.put("amount", event.getAmount());
        data.put("description", event.getDescription());
        data.put("posterUrl", event.getPosterUrl());
        data.put("sampleSize", event.getSampleSize());
        docRef.set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore", "DocumentSnapshot successfully written!");
                    }
                });

    }

    @Override
    public void onConfirmPressed(String eventName) {
        eventsRef.document(eventName).delete()
                .addOnFailureListener(e -> {
                    // If doc ID is UUID, try query by name
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
