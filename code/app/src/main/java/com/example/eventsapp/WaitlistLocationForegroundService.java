package com.example.eventsapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps publishing the entrant's device location to Firestore while they are on a waitlist,
 * so organizers see near–real-time positions on the map even when this app is in the background.
 */
public class WaitlistLocationForegroundService extends Service {

    private static final String TAG = "WaitlistLocService";

    static final String ACTION_STOP = "com.example.eventsapp.STOP_WAITLIST_LOCATION";

    static final String EXTRA_EVENT_ID = "extra_event_id";
    static final String EXTRA_ENTRANT_DOC_ID = "extra_entrant_doc_id";
    static final String EXTRA_EVENT_NAME = "extra_event_name";

    private static final String CHANNEL_ID = "waitlist_location_channel";
    private static final int NOTIFICATION_ID = 0x5741544c; // "WATL"

    private static final long UPDATE_INTERVAL_MS = 12_000L;
    private static final long MIN_WRITE_INTERVAL_MS = 10_000L;

    private final LocationRequest locationRequest =
            new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                    .setMinUpdateIntervalMillis(8_000L)
                    .setMinUpdateDistanceMeters(10f)
                    .build();

    private FusedLocationProviderClient fused;
    private HandlerThread workerThread;
    private Looper workerLooper;
    private FirebaseFirestore db;

    private volatile String eventId;
    private volatile String entrantDocId;
    private volatile long lastFirestoreWriteMs;

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {
            if (eventId == null || entrantDocId == null || eventId.isEmpty() || entrantDocId.isEmpty()) {
                return;
            }
            Location loc = result.getLastLocation();
            if (loc == null) return;

            long now = System.currentTimeMillis();
            if (now - lastFirestoreWriteMs < MIN_WRITE_INTERVAL_MS) {
                return;
            }
            lastFirestoreWriteMs = now;

            Map<String, Object> patch = new HashMap<>();
            patch.put("latitude", loc.getLatitude());
            patch.put("longitude", loc.getLongitude());
            patch.put("locationUpdatedAt", FieldValue.serverTimestamp());
            if (loc.hasAccuracy()) {
                patch.put("locationAccuracy", loc.getAccuracy());
            }

            db.collection("events").document(eventId)
                    .collection("entrants").document(entrantDocId)
                    .update(patch)
                    .addOnFailureListener(e -> Log.w(TAG, "Firestore location update failed", e));
        }
    };

    /**
     * Convenience factory that starts the foreground service with the required extras.
     *
     * <p>Calling this method when the service is already running simply delivers a new
     * {@link #onStartCommand} which updates {@link #eventId} and {@link #entrantDocId} and
     * resets the location-update stream for the new event.
     *
     * @param context          Application or activity context; must not be {@code null}.
     * @param eventId          Firestore event document ID; must not be {@code null} or empty.
     * @param entrantDocId     Firestore entrant document ID within the event; must not be
     *                         {@code null} or empty.
     * @param eventDisplayName Human-readable event name shown in the persistent notification;
     *                         {@code null} is treated as an empty string.
     */
    public static void start(Context context, String eventId, String entrantDocId, String eventDisplayName) {
        if (context == null || eventId == null || entrantDocId == null
                || eventId.isEmpty() || entrantDocId.isEmpty()) {
            return;
        }
        Intent i = new Intent(context, WaitlistLocationForegroundService.class);
        i.putExtra(EXTRA_EVENT_ID, eventId);
        i.putExtra(EXTRA_ENTRANT_DOC_ID, entrantDocId);
        i.putExtra(EXTRA_EVENT_NAME, eventDisplayName != null ? eventDisplayName : "");
        ContextCompat.startForegroundService(context, i);
    }

    /**
     * Stops the foreground service and removes the persistent notification.
     *
     * <p>Safe to call even when the service is not running; the system will silently ignore
     * a stop request for a service that is already stopped.
     *
     * @param context Application or activity context; ignored if {@code null}.
     */
    public static void stop(Context context) {
        if (context == null) return;
        context.stopService(new Intent(context, WaitlistLocationForegroundService.class));
    }

    /**
     * Initialises the Fused Location Provider, Firestore client, and the background
     * {@link HandlerThread} used to receive location callbacks off the main thread.
     * Also ensures the notification channel exists on API 26+.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (isGooglePlayServicesLocationAvailable()) {
            fused = LocationServices.getFusedLocationProviderClient(this);
        }
        db = FirebaseFirestore.getInstance();
        workerThread = new HandlerThread("waitlist-loc-worker");
        workerThread.start();
        workerLooper = workerThread.getLooper();
        createNotificationChannel();
    }

    /**
     * Handles both start and stop commands.
     *
     * <ul>
     *   <li>If {@code intent.getAction()} equals {@link #ACTION_STOP}, the service tears itself
     *       down immediately.</li>
     *   <li>Otherwise, the extras {@link #EXTRA_EVENT_ID} and {@link #EXTRA_ENTRANT_DOC_ID} are
     *       read, the service promotes itself to the foreground with a persistent notification,
     *       and high-accuracy location updates are requested at {@link #UPDATE_INTERVAL_MS} ms
     *       intervals.</li>
     * </ul>
     *
     * @param intent  The starting intent, or {@code null} if the system restarts a sticky service.
     * @param flags   Delivery flags as defined by {@link Service#onStartCommand}.
     * @param startId Unique ID for this start request.
     * @return {@link Service#START_STICKY} so the OS restarts the service if it is killed,
     *         or {@link Service#START_NOT_STICKY} in error cases.
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopLocationUpdatesInternal();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String newEventId = intent.getStringExtra(EXTRA_EVENT_ID);
        String newDocId = intent.getStringExtra(EXTRA_ENTRANT_DOC_ID);
        String eventName = intent.getStringExtra(EXTRA_EVENT_NAME);
        if (newEventId == null || newDocId == null || newEventId.isEmpty() || newDocId.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        stopLocationUpdatesInternal();
        this.eventId = newEventId;
        this.entrantDocId = newDocId;
        this.lastFirestoreWriteMs = 0L;

        Notification notification = buildNotification(eventName != null ? eventName : "");
        startForeground(NOTIFICATION_ID, notification);

        if (fused == null || !isGooglePlayServicesLocationAvailable()) {
            Log.w(TAG, "Fused location unavailable; stopping waitlist location service");
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            fused.requestLocationUpdates(locationRequest, locationCallback, workerLooper)
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to request fused location updates", e);
                        stopForeground(STOP_FOREGROUND_REMOVE);
                        stopSelf();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Missing location permission", e);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    /**
     * Cleans up by removing location update callbacks and gracefully quitting the background
     * {@link HandlerThread} to avoid thread leaks.
     */
    @Override
    public void onDestroy() {
        stopLocationUpdatesInternal();
        if (workerThread != null) {
            workerThread.quitSafely();
        }
        super.onDestroy();
    }

    /**
     * This service does not support binding; always returns {@code null}.
     *
     * @param intent The intent used to bind (ignored).
     * @return {@code null}.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Removes the registered {@link LocationCallback} from the Fused Location Provider.
     * Safe to call even if {@link #fused} was never initialised.
     */
    private void stopLocationUpdatesInternal() {
        if (fused != null) {
            fused.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Creates the {@value #CHANNEL_ID} {@link NotificationChannel} with
     * {@link android.app.NotificationManager#IMPORTANCE_LOW} importance (silent, no pop-over).
     * No-op on devices below API 26.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_waitlist_location),
                NotificationManager.IMPORTANCE_LOW);
        ch.setDescription(getString(R.string.notif_channel_waitlist_location_desc));
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(ch);
        }
    }

    /**
     * Constructs the persistent foreground {@link Notification} shown while location sharing is
     * active.  Includes a "Stop" action that sends {@link #ACTION_STOP} to the service.
     *
     * @param eventDisplayName The event name to show as the notification sub-text; an empty string
     *                         causes a generic fallback string to be used instead.
     * @return A fully configured, non-dismissible {@link Notification}.
     */
    private Notification buildNotification(String eventDisplayName) {
        Intent stopIntent = new Intent(this, WaitlistLocationForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String subtitle = eventDisplayName.isEmpty()
                ? getString(R.string.notif_waitlist_location_generic)
                : eventDisplayName;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_waitlist_location_title))
                .setContentText(subtitle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, getString(R.string.notif_waitlist_location_stop), stopPi)
                .build();
    }

    /**
     * Checks whether Google Play Services is available and up-to-date on this device.
     *
     * @return {@code true} if the Fused Location Provider can be used; {@code false} otherwise
     *         (emulators without Play Services, certain custom ROMs, etc.).
     */
    private boolean isGooglePlayServicesLocationAvailable() {
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        return status == ConnectionResult.SUCCESS;
    }
}
