package com.example.eventsapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements EventDialogFragment.EventDialogListener, DeleteEventDialogFragment.OnFragmentInteractionListener {

    private Button addEventButton;
    private Button deleteEventButton;

    private ListView eventListView;

    private ArrayList<Event> eventArrayList;
    private ArrayAdapter<Event> eventArrayAdapter;

    private FirebaseFirestore db;

    private CollectionReference eventsRef;
import com.google.android.material.bottomnavigation.BottomNavigationView;

    public class MainActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(res.layout.activity_main);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            // Set views
            addEventButton = findViewById(R.id.buttonAddEvent);
            deleteEventButton = findViewById(R.id.buttonDeleteEvent);
            eventListView = findViewById(R.id.listviewCities);


            // create event array
            eventArrayList = new ArrayList<>();
            eventArrayAdapter = new EventArrayAdapter(this, eventArrayList);
            eventListView.setAdapter(eventArrayAdapter);

            // set listeners
            addEventButton.setOnClickListener(view -> {
                EventDialogFragment eventDialogFragment = new EventDialogFragment();
                eventDialogFragment.show(getSupportFragmentManager(), "Add Event");
            });

            deleteEventButton.setOnClickListener(view -> {
                DeleteEventDialogFragment deleteEventDialogFragment = new DeleteEventDialogFragment();
                deleteEventDialogFragment.show(getSupportFragmentManager(), "Delete Event");
            });

            eventListView.setOnItemClickListener((adapterView, view, i, l) -> {
                Event event = eventArrayAdapter.getItem(i);
                EventDialogFragment eventDialogFragment = EventDialogFragment.newInstance(event);
                eventDialogFragment.show(getSupportFragmentManager(), "Event Details");
            });

            db = FirebaseFirestore.getInstance();
            eventsRef = db.collection("events");

            eventsRef.addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e("Firestore", error.toString());
                    return;
                }
                if (value != null) {
                    eventArrayList.clear();
                    for (QueryDocumentSnapshot snapshot : value) {
                        String name = snapshot.getString("name");
                        Long amountLong = snapshot.getLong("amount");
                        int amount = (amountLong != null) ? amountLong.intValue() : 0;

                        if (amount != 0) {
                            eventArrayList.add(new Event(name, amount));
                        }
                    }
                    eventArrayAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void updateEvent(Event event, String title, int amount) {
            event.setName(title);
            event.setAmount(amount);
            eventArrayAdapter.notifyDataSetChanged();

            DocumentReference docRef = eventsRef.document(event.getName());
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
        public void addEvent(Event event) {
            eventArrayList.add(event);
            eventArrayAdapter.notifyDataSetChanged();

            DocumentReference docRef = eventsRef.document(event.getName());
            docRef.set(event)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Firestore", "DocumentSnapshot successfully written!");
                        }
                    });

        }

        @Override
        public void onConfirmPressed(String eventName) {
            eventsRef.document(eventName).delete();

            BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

            NavHostFragment navHost =
                    (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

            NavController navController = navHost.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }
}
