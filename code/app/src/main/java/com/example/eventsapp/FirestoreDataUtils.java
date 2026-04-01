package com.example.eventsapp;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

final class FirestoreDataUtils {

    private FirestoreDataUtils() {}

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
