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

/**
 * The primary host activity of the application.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Hosts the {@link androidx.navigation.fragment.NavHostFragment} and wires it to the
 *       bottom navigation bar.</li>
 *   <li>Hides the bottom bar on full-screen destinations (sign-in, event detail, create/edit).</li>
 *   <li>Handles deep-link intents ({@code tigers-events://event/<id>}) and navigates directly to
 *       {@code EventDetailFragment}.</li>
 *   <li>Implements {@link EventDialogFragment.EventDialogListener} and
 *       {@link DeleteEventDialogFragment.OnFragmentInteractionListener} to persist event
 *       mutations originating from dialog fragments.</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity implements EventDialogFragment.EventDialogListener, DeleteEventDialogFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private FirebaseFirestore db;
    private CollectionReference eventsRef;
    private CollectionReference userRef;

    /**
     * Sets up the navigation graph, bottom-navigation bar, destination-change listener, and
     * processes any deep-link that was present in the launching {@link android.content.Intent}.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null} on first launch.
     */
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

    /**
     * Adds a new user document to the {@code users} Firestore collection if no document with the
     * same e-mail address already exists.
     *
     * <p>The document ID is auto-generated and written back into {@link Users#setId(String)} before
     * the document is persisted, so the ID is available server-side as well as locally.
     *
     * @param user The user to persist; must have a non-null e-mail address.
     */
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

    /**
     * Updates an existing event's name and capacity in Firestore.
     *
     * <p>The document ID is resolved from {@link Event#getId()} first, falling back to
     * {@link Event#getName()} for legacy documents that lack an explicit ID field. No write is
     * performed if the resolved ID is blank.
     *
     * @param event  The event object to update; must not be {@code null}.
     * @param title  The new event name.
     * @param amount The new attendee capacity.
     */
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

    /**
     * Persists a newly created event to Firestore.
     *
     * <p>Uses {@link Event#getId()} as the document ID when present, otherwise falls back to
     * {@link Event#getName()}. Delegates to {@link #saveEvent(String, Event)}.
     *
     * @param event The event to save; must not be {@code null}.
     */
    @Override
    public void addEvent(Event event) {
        if (event == null) {
            Log.w(TAG, "addEvent called with null event");
            return;
        }

        String docId = (event.getId() != null && !event.getId().isEmpty()) ? event.getId() : event.getName();
        saveEvent(docId, event);
    }

    /**
     * Called when the user confirms deletion of an event from the delete-event dialog.
     *
     * <p>Looks up the event first by document ID (fast path), then by name field (fallback for
     * older documents), and delegates to {@link EventCleanupHelper#deleteEventCompletely} to
     * atomically remove the event and all related sub-collections.
     *
     * @param eventName The name of the event to delete; trimmed of surrounding whitespace.
     */
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

    /**
     * Handles deep-link intents delivered to an already-running activity instance (e.g. when the
     * app is in the back-stack and a QR code is scanned again).
     *
     * @param intent The new intent containing an optional deep-link URI.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    /**
     * Inspects the intent's data URI for the {@code tigers-events://event/<id>} scheme and, if
     * matched, navigates the NavController directly to {@code EventDetailFragment} with the
     * extracted event ID passed as an argument.
     *
     * <p>Navigation is posted to the next UI frame to ensure the NavController is fully
     * initialised before {@link androidx.navigation.NavController#navigate} is called.
     *
     * @param intent The intent to inspect; ignored if {@code null} or has no data URI.
     */
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

    /**
     * Writes (or overwrites) the given event to Firestore under the specified document ID.
     *
     * <p>Only a fixed set of known fields is written so that unrelated Firestore fields (e.g.
     * sub-collection metadata) are not accidentally erased by a partial update.
     *
     * @param documentId The Firestore document ID to write to; must be non-null and non-empty.
     * @param event      The event whose data should be persisted.
     */
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
