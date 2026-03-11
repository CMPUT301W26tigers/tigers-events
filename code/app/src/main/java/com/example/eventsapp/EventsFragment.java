package com.example.eventsapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class EventsFragment extends Fragment {
    private ArrayList<Event> eventArrayList = new ArrayList<>();
    private ArrayAdapter<Event> eventArrayAdapter;

    public EventsFragment() { super(R.layout.fragment_events); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listEvents = view.findViewById(R.id.list_events);
        eventArrayAdapter = new EventArrayAdapter(requireContext(), eventArrayList);
        listEvents.setAdapter(eventArrayAdapter);

        view.findViewById(R.id.btn_create_event).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.createEventFragment));

        listEvents.setOnItemClickListener((parent, v, position, id) -> {
            Event event = eventArrayAdapter.getItem(position);
            EventDialogFragment.newInstance(event).show(requireActivity().getSupportFragmentManager(), "Event Details");
        });

        CollectionReference eventsRef = FirebaseFirestore.getInstance().collection("events");
        eventsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("EventsFragment", error.toString());
                return;
            }
            if (value != null && isAdded()) {
                eventArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String id = snapshot.getString("id");
                    String name = snapshot.getString("name");
                    Long amountLong = snapshot.getLong("amount");
                    int amount = (amountLong != null) ? amountLong.intValue() : 0;
                    String description = snapshot.getString("description");
                    String posterUrl = snapshot.getString("posterUrl");
                    Long sampleLong = snapshot.getLong("sampleSize");
                    int sampleSize = (sampleLong != null) ? sampleLong.intValue() : 0;
                    if (id == null) id = snapshot.getId();
                    if (name == null) name = "";
                    if (description == null) description = "";
                    if (posterUrl == null) posterUrl = "";

                    if (amount != 0) {
                        eventArrayList.add(new Event(id, name, amount, description, posterUrl, sampleSize));
                    }
                }
                eventArrayAdapter.notifyDataSetChanged();
            }
        });
    }
}
