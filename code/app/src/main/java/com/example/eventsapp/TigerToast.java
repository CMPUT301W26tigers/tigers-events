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

    private TigerToast() {}

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

    public static Toast show(@NonNull Context context, @StringRes int resId, int duration) {
        return show(context, context.getString(resId), duration);
    }
}
