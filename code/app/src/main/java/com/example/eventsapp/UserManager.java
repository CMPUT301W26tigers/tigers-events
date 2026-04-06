package com.example.eventsapp;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton manager class for handling the current user session and authentication state.
 * This class provides a centralized point to access, set, and clear the logged-in user.
 */
public class UserManager {
    public interface SessionBootstrapCallback {
        void onBootstrapComplete(@Nullable Users user);
    }

    private static UserManager instance;
    private Users currentUser;
    private boolean sessionBootstrapComplete;
    private boolean sessionBootstrapInProgress;
    private final List<SessionBootstrapCallback> pendingBootstrapCallbacks = new ArrayList<>();

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private UserManager() {}

    /**
     * Gets the singleton instance of UserManager.
     *
     * @return The UserManager instance.
     */
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Gets the currently logged-in user.
     *
     * @return The current Users object, or null if no user is logged in.
     */
    public Users getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isSessionBootstrapComplete() {
        return sessionBootstrapComplete;
    }

    public synchronized boolean isSessionBootstrapInProgress() {
        return sessionBootstrapInProgress;
    }

    /**
     * Sets the current user upon login or sign-up.
     *
     * @param user The Users object to set as the current user.
     */
    public synchronized void setCurrentUser(Users user) {
        this.currentUser = user;
        this.sessionBootstrapComplete = true;
    }

    /**
     * Checks if a user is currently logged into the application.
     *
     * @return True if a user is logged in, false otherwise.
     */
    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public void bootstrapSession(@NonNull Context context, @NonNull SessionBootstrapCallback callback) {
        SessionBootstrapCallback[] callbacksToRunNow = null;
        synchronized (this) {
            if (sessionBootstrapComplete) {
                callbacksToRunNow = new SessionBootstrapCallback[]{callback};
            } else {
                pendingBootstrapCallbacks.add(callback);
                if (sessionBootstrapInProgress) {
                    return;
                }
                sessionBootstrapInProgress = true;
            }
        }

        if (callbacksToRunNow != null) {
            callback.onBootstrapComplete(getCurrentUser());
            return;
        }

        String deviceId = Settings.Secure.getString(
                context.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        if (deviceId == null || deviceId.isEmpty()) {
            finishBootstrap(null);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Users resolvedUser = null;
                    if (!querySnapshot.isEmpty()) {
                        resolvedUser = querySnapshot.getDocuments().get(0).toObject(Users.class);
                        if (resolvedUser != null) {
                            resolvedUser.setId(querySnapshot.getDocuments().get(0).getId());
                        }
                    }
                    finishBootstrap(resolvedUser);
                })
                .addOnFailureListener(e -> finishBootstrap(null));
    }

    /**
     * Logs out the current user by clearing the session.
     */
    public synchronized void logout() {
        currentUser = null;
        sessionBootstrapComplete = true;
    }

    private void finishBootstrap(@Nullable Users user) {
        List<SessionBootstrapCallback> callbacks;
        synchronized (this) {
            currentUser = user;
            sessionBootstrapComplete = true;
            sessionBootstrapInProgress = false;
            callbacks = new ArrayList<>(pendingBootstrapCallbacks);
            pendingBootstrapCallbacks.clear();
        }

        for (SessionBootstrapCallback callback : callbacks) {
            callback.onBootstrapComplete(user);
        }
    }
}
