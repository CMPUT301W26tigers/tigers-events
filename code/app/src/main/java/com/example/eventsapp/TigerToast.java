package com.example.eventsapp;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/**
 * Drop-in replacement for {@link Toast#makeText} that shows the tiger logo
 * alongside the message in a styled pill-shaped card.
 */
public final class TigerToast {

    /** Utility class — not instantiable. */
    private TigerToast() {}

    /**
     * Inflates the branded toast layout, sets the message, and shows the toast.
     *
     * @param context  The context used to inflate the layout and display the toast.
     * @param message  The text to display inside the toast.
     * @param duration {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}.
     * @return The {@link Toast} instance after {@link Toast#show()} has been called,
     *         in case the caller needs to cancel it early.
     */
    public static Toast show(@NonNull Context context, @NonNull CharSequence message, int duration) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast_tiger, null);
        ((TextView) layout.findViewById(R.id.toast_text)).setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
        toast.setView(layout);
        toast.show();
        return toast;
    }

    /**
     * Convenience overload that resolves a string resource before delegating to
     * {@link #show(Context, CharSequence, int)}.
     *
     * @param context  The context used to resolve the string resource and display the toast.
     * @param resId    The string resource ID of the message to display.
     * @param duration {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}.
     * @return The {@link Toast} instance after {@link Toast#show()} has been called.
     */
    public static Toast show(@NonNull Context context, @StringRes int resId, int duration) {
        return show(context, context.getString(resId), duration);
    }
}
