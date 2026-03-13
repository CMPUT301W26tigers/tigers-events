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

/**
 * A dialog fragment for adding or editing basic event details (name and capacity).
 * It communicates with the host through the {@link EventDialogListener} interface.
 */
public class EventDialogFragment extends DialogFragment {

    /**
     * Interface for receiving event addition or update callbacks.
     */
    interface EventDialogListener {
        /**
         * Called when an existing event is updated.
         * @param event The original event being updated.
         * @param title The new title for the event.
         * @param amount The new capacity for the event.
         */
        void updateEvent(Event event, String title, int amount);

        /**
         * Called when a new event is added.
         * @param event The new event object.
         */
        void addEvent(Event event);
    }

    private EventDialogListener listener;

    /**
     * Creates a new instance of EventDialogFragment for editing an existing event.
     * @param event The event to be edited.
     * @return A new instance of EventDialogFragment.
     */
    public static EventDialogFragment newInstance(Event event){
        Bundle args = new Bundle();
        args.putSerializable("Event", event);

        EventDialogFragment fragment = new EventDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called when a fragment is first attached to its context.
     * @param context The context to attach to.
     * @throws RuntimeException if the context does not implement {@link EventDialogListener}.
     */
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

    /**
     * Override to build your own custom Dialog container.
     * @param savedInstanceState The last saved instance state of the Fragment.
     * @return Return a new Dialog instance to be displayed by the Fragment.
     */
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
