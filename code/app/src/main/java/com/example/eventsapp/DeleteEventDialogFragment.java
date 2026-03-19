package com.example.eventsapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * A dialog fragment that prompts the user to confirm the deletion of an event.
 * It requires the user to input the event name to confirm the action.
 */
public class DeleteEventDialogFragment extends DialogFragment {

    private EditText eventNameEditText;
    private OnFragmentInteractionListener listener;

    /**
     * Interface for receiving deletion confirmation events.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Called when the user confirms the deletion of an event.
         * @param eventName The name of the event as entered by the user.
         */
        void onConfirmPressed(String eventName);
    }

    /**
     * Called when a fragment is first attached to its context.
     * Ensures that the host context implements {@link OnFragmentInteractionListener}.
     *
     * @param context The context to attach to.
     * @throws RuntimeException if the context does not implement {@link OnFragmentInteractionListener}.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context
                    + " must implement OnFragmentInteractionListener");
        }
    }

    /**
     * Override to build your own custom Dialog container.
     * Inflates the delete event layout and sets up the positive and negative buttons.
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     * or null if this is a new Fragment.
     * @return Return a new Dialog instance to be displayed by the Fragment.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_delete_event, null);
        eventNameEditText = view.findViewById(R.id.editTextEventName);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        return builder
                .setView(view)
                .setTitle("Delete Event")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String eventName = eventNameEditText.getText().toString();
                    listener.onConfirmPressed(eventName);
                })
                .create();
    }
}
