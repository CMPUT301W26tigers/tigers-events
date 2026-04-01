package com.example.eventsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminManageNotificationsFragment extends Fragment {
    private static final String TAG = "AdminManageNotifications";

    private final List<NotificationLogItem> allLogs = new ArrayList<>();
    private final List<NotificationLogItem> filteredLogs = new ArrayList<>();
    private AdminNotificationLogAdapter adapter;
    private String currentQuery = "";

    public AdminManageNotificationsFragment() {
        super(R.layout.fragment_admin_manage_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBackToUser).setOnClickListener(v -> {
            requireActivity().finish();
        });

        RecyclerView rv = view.findViewById(R.id.rvAdminNotifications);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminNotificationLogAdapter(filteredLogs);
        rv.setAdapter(adapter);

        setupSearch(view);
        loadNotificationLogs();
    }

    private void setupSearch(View view) {
        EditText etSearch = view.findViewById(R.id.etSearchNotifications);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString();
                applyFilter(currentQuery);
            }
        });
    }

    private void loadNotificationLogs() {
        FirebaseFirestore.getInstance().collection("notification_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;
                    allLogs.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        NotificationLogItem log = doc.toObject(NotificationLogItem.class);
                        allLogs.add(log);
                    }
                    applyFilter(currentQuery);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load notification logs", e);
                });
    }

    private void applyFilter(String query) {
        filteredLogs.clear();
        String lowerQuery = query.trim().toLowerCase();

        for (NotificationLogItem log : allLogs) {
            if (lowerQuery.isEmpty()
                    || safeContains(log.getOrganizerName(), lowerQuery)
                    || safeContains(log.getEventName(), lowerQuery)
                    || safeContains(log.getTitle(), lowerQuery)
                    || safeContains(log.getMessage(), lowerQuery)) {
                filteredLogs.add(log);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private boolean safeContains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
