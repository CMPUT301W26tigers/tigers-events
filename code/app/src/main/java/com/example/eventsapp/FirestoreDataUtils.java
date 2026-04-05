package com.example.eventsapp;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Package-private utility class providing safe, typed accessors for Firestore document fields.
 * All methods are stateless and static; instances cannot be created.
 */
final class FirestoreDataUtils {

    private FirestoreDataUtils() {}

    /**
     * Reads a Firestore array field as a {@code List<String>}, skipping any non-string or
     * blank entries.
     *
     * <p>Returns an empty list rather than {@code null} when the field is absent, not an
     * array, or contains no valid string values, so callers can safely iterate without null
     * checks.
     *
     * @param snapshot the Firestore document to read from
     * @param field    the name of the array field
     * @return a non-null list of non-blank strings found in the field
     */
    @NonNull
    static List<String> getStringList(@NonNull DocumentSnapshot snapshot, @NonNull String field) {
        Object rawValue = snapshot.get(field);
        if (!(rawValue instanceof List<?>)) {
            return new ArrayList<>();
        }

        List<?> rawList = (List<?>) rawValue;
        List<String> values = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            if (item instanceof String) {
                String value = (String) item;
                if (!value.trim().isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values;
    }
}
