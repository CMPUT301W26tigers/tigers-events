package com.example.eventsapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class EventDialogFragment extends DialogFragment {
    interface EventDialogListener {
        void updateEvent(Event event, String title, int amount);
        void addEvent(Event event);
    }
    private EventDialogListener listener;

    public static EventDialogFragment newInstance(Event event){
        Bundle args = new Bundle();
        args.putSerializable("Event", event);

        EventDialogFragment fragment = new EventDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof EventDialogListener){
            listener = (EventDialogListener) context;
        }
        else {
            throw new RuntimeException("Implement listener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_event_details, null);
        EditText editEventName = view.findViewById(R.id.edit_event_name);
        EditText editAmount = view.findViewById(R.id.edit_amount);

        String tag = getTag();
        Bundle bundle = getArguments();
        Event event;

        if (Objects.equals(tag, "Event Details") && bundle != null){
            event = (Event) bundle.getSerializable("Event");
            assert event != null;
            editEventName.setText(event.getName());
            editAmount.setText(String.valueOf(event.getAmount()));
        }
        else {
            event = null;}

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Event Details")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (dialog, which) -> {
                    String title = editEventName.getText().toString();
                    String amountStr = editAmount.getText().toString();
                    int amount;
                    try {
                        amount = Integer.parseInt(amountStr);
                        if (amount == 0) {
                            Toast.makeText(getContext(), "Amount cannot be zero", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Amount must be an integer", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (Objects.equals(tag, "Event Details")) {
                        listener.updateEvent(event, title, amount);
                    } else {
                        listener.addEvent(new Event(title, amount));
                    }
                })
                .create();
    }
}
