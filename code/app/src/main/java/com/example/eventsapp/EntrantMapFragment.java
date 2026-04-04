package com.example.eventsapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OSMDroid map of entrant join locations read only from Firestore (latitude / longitude fields).
 */
public class EntrantMapFragment extends Fragment {

    private static final String TAG = "EntrantMapFragment";

    /** Isolated from app-wide default prefs so OSMDroid does not restore another screen's map center. */
    private static final String OSM_PREFS_NAME = "osmdroid_entrant_map";

    /** ~25 m per step so stacked testers at the same GPS fix still get two visible pins. */
    private static final double DUPLICATE_OFFSET_STEP = 0.00025;

    private String eventId;
    private String eventName;
    private boolean mapFocusEntrant;
    private String focusEntrantDocId;
    private String focusName;

    private MapView mapView;
    private TextView tvViewable;
    private TextView tvSubtitle;
    private ListenerRegistration registration;

    /** Camera derived from Firestore; reapplied after {@link MapView#onResume()} so OSMDroid cannot overwrite it. */
    @Nullable
    private GeoPoint savedCameraCenter;
    private double savedCameraZoom = Double.NaN;
    @Nullable
    private BoundingBox savedCameraBounds;
    private boolean savedCameraUseBounds;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString("eventId", "");
            eventName = args.getString("eventName");
            mapFocusEntrant = args.getBoolean("mapFocusEntrant", false);
            focusEntrantDocId = args.getString("focusEntrantDocId");
            focusName = args.getString("focusName");
        } else {
            eventId = "";
        }

        if (getContext() != null) {
            Context ctx = getContext();
            SharedPreferences osmPrefs = ctx.getSharedPreferences(OSM_PREFS_NAME, Context.MODE_PRIVATE);
            Configuration.getInstance().load(ctx, osmPrefs);
            Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_map);
        toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        tvSubtitle = view.findViewById(R.id.tv_map_subtitle);
        tvViewable = view.findViewById(R.id.tv_viewable_locations);
        mapView = view.findViewById(R.id.map);
        MaterialButton btnBack = view.findViewById(R.id.btn_back_waitlist);
        btnBack.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        if (eventName != null && !eventName.isEmpty()) {
            tvSubtitle.setText(eventName);
        } else {
            tvSubtitle.setVisibility(View.GONE);
        }

        mapView.setMultiTouchControls(true);
        mapView.setSaveEnabled(false);

        if (eventId.isEmpty()) {
            TigerToast.show(requireContext(), "Missing event", Toast.LENGTH_SHORT);
            return;
        }

        Query query = FirebaseFirestore.getInstance()
                .collection("events").document(eventId).collection("entrants")
                .whereIn("status", Arrays.asList("APPLIED", "INVITED", "ACCEPTED"));

        // Avoid showing stale local-cache coordinates: paint only after SERVER read completes,
        // then keep a snapshot listener for live updates.
        query.get(Source.SERVER).addOnCompleteListener(task -> {
            if (!isAdded() || mapView == null) {
                return;
            }
            if (task.isSuccessful()) {
                QuerySnapshot snap = task.getResult();
                Log.d(TAG, "SERVER fetch complete; fromCache=" + snap.getMetadata().isFromCache()
                        + " docs=" + snap.size());
                applyEntrantsSnapshot(snap);
            } else {
                Log.e(TAG, "SERVER fetch failed; no markers until listener fires", task.getException());
            }

            if (registration != null) {
                registration.remove();
                registration = null;
            }
            if (!isAdded() || mapView == null) {
                return;
            }
            registration = query.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "entrants listen failed", error);
                    return;
                }
                if (!isAdded() || mapView == null || snapshot == null) {
                    return;
                }
                Log.d(TAG, "Snapshot update; fromCache=" + snapshot.getMetadata().isFromCache()
                        + " docs=" + snapshot.size());
                applyEntrantsSnapshot(snapshot);
            });
        });
    }

    /**
     * Builds markers and camera strictly from this Firestore snapshot (no device GPS).
     */
    private void applyEntrantsSnapshot(@NonNull QuerySnapshot snapshot) {
        mapView.getOverlays().clear();
        List<GeoPoint> markerPositions = new ArrayList<>();
        List<double[]> rawLatLng = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<QueryDocumentSnapshot> geocodedDocs = new ArrayList<>();

        for (QueryDocumentSnapshot doc : snapshot) {
            String docId = doc.getId();
            double lat = readLatitude(doc);
            double lng = readLongitude(doc);
            Log.d(TAG, "Firebase doc=" + docId + " name=" + doc.getString("name")
                    + " lat=" + lat + " lng=" + lng);

            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                continue;
            }
            if (Math.abs(lat) > 90 || Math.abs(lng) > 180) {
                continue;
            }
            if (lat == 0.0 && lng == 0.0) {
                continue;
            }

            String title = doc.getString("name");
            if (title == null || title.isEmpty()) {
                title = "Entrant";
            }

            rawLatLng.add(new double[]{lat, lng});
            titles.add(title);
            geocodedDocs.add(doc);
        }

        List<GeoPoint> plotPoints = spreadDuplicateCoordinates(rawLatLng);
        for (int i = 0; i < plotPoints.size(); i++) {
            GeoPoint gp = plotPoints.get(i);
            Log.d(TAG, "Marker i=" + i + " position lat=" + gp.getLatitude() + " lng=" + gp.getLongitude()
                    + " (from Firebase lat=" + rawLatLng.get(i)[0] + " lng=" + rawLatLng.get(i)[1] + ")");

            Marker marker = new Marker(mapView);
            marker.setPosition(gp);
            marker.setTitle(titles.get(i));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
            markerPositions.add(gp);
        }

        int n = markerPositions.size();
        tvViewable.setText(getString(R.string.viewable_locations_count, n));

        if (markerPositions.isEmpty()) {
            clearSavedFirestoreCamera();
            Log.d(TAG, "No valid coordinates in snapshot; camera cleared");
            mapView.invalidate();
            return;
        }

        GeoPoint cameraTarget = null;
        double cameraZoom = 14.0;

        if (mapFocusEntrant && focusEntrantDocId != null && !focusEntrantDocId.isEmpty()) {
            int focusIdx = -1;
            for (int i = 0; i < geocodedDocs.size(); i++) {
                QueryDocumentSnapshot d = geocodedDocs.get(i);
                if (focusEntrantDocId.equals(d.getId())) {
                    focusIdx = i;
                    break;
                }
                String idField = d.getString("id");
                if (focusEntrantDocId.equals(idField)) {
                    focusIdx = i;
                    break;
                }
            }
            if (focusIdx >= 0) {
                cameraTarget = markerPositions.get(focusIdx);
                cameraZoom = 15.0;
                if (focusName != null && !focusName.isEmpty()) {
                    Log.d(TAG, "Focus mode for entrant=" + focusName + " docId=" + focusEntrantDocId);
                }
            } else {
                Log.w(TAG, "focusEntrantDocId not in snapshot; fitting all markers");
            }
        }

        if (cameraTarget != null) {
            Log.d(TAG, "Camera (focus) lat=" + cameraTarget.getLatitude() + " lng="
                    + cameraTarget.getLongitude() + " zoom=" + cameraZoom);
            rememberFirestoreCamera(cameraTarget, cameraZoom, null, false);
        } else if (markerPositions.size() == 1) {
            GeoPoint c = markerPositions.get(0);
            Log.d(TAG, "Camera (single) lat=" + c.getLatitude() + " lng=" + c.getLongitude()
                    + " zoom=" + cameraZoom);
            rememberFirestoreCamera(c, cameraZoom, null, false);
        } else {
            BoundingBox box = boundingBoxWithMinimumSpan(markerPositions, 0.02, 0.02);
            Log.d(TAG, "Camera (bounds) box N=" + box.getLatNorth() + " S=" + box.getLatSouth()
                    + " E=" + box.getLonEast() + " W=" + box.getLonWest());
            rememberFirestoreCamera(null, Double.NaN, box, true);
        }

        scheduleReapplyFirestoreCamera();
    }

    private void clearSavedFirestoreCamera() {
        savedCameraCenter = null;
        savedCameraZoom = Double.NaN;
        savedCameraBounds = null;
        savedCameraUseBounds = false;
    }

    private void rememberFirestoreCamera(@Nullable GeoPoint center, double zoom,
                                        @Nullable BoundingBox bounds, boolean useBounds) {
        savedCameraUseBounds = useBounds;
        savedCameraBounds = bounds;
        savedCameraCenter = center;
        savedCameraZoom = zoom;
    }

    /**
     * OSMDroid restores scroll/zoom during {@link MapView#onResume()}; re-apply after layout so the
     * viewport matches Firestore coordinates, not a persisted default.
     */
    private void scheduleReapplyFirestoreCamera() {
        if (mapView == null) {
            return;
        }
        mapView.post(this::applySavedFirestoreCameraNow);
        mapView.postDelayed(this::applySavedFirestoreCameraNow, 50L);
    }

    private void applySavedFirestoreCameraNow() {
        if (mapView == null || !isAdded()) {
            return;
        }
        if (!savedCameraUseBounds && savedCameraCenter == null) {
            return;
        }
        if (savedCameraUseBounds) {
            if (savedCameraBounds == null) {
                return;
            }
            mapView.zoomToBoundingBox(savedCameraBounds, true, 96);
            IGeoPoint ctr = mapView.getMapCenter();
            Log.d(TAG, "Reapplied camera (bounds) center lat=" + ctr.getLatitude() + " lng="
                    + ctr.getLongitude() + " zoom=" + mapView.getZoomLevelDouble());
        } else if (savedCameraCenter != null && !Double.isNaN(savedCameraZoom)) {
            mapView.getController().setZoom(savedCameraZoom);
            mapView.getController().setCenter(savedCameraCenter);
            Log.d(TAG, "Reapplied camera (center) lat=" + savedCameraCenter.getLatitude() + " lng="
                    + savedCameraCenter.getLongitude() + " zoom=" + savedCameraZoom);
        }
        mapView.invalidate();
    }

    private static double readLatitude(DocumentSnapshot doc) {
        Object o = doc.get("latitude");
        Double d = objectToDouble(o);
        if (d != null) {
            return d;
        }
        com.google.firebase.firestore.GeoPoint gp = doc.getGeoPoint("location");
        if (gp != null) {
            return gp.getLatitude();
        }
        return Double.NaN;
    }

    private static double readLongitude(DocumentSnapshot doc) {
        Object o = doc.get("longitude");
        Double d = objectToDouble(o);
        if (d != null) {
            return d;
        }
        com.google.firebase.firestore.GeoPoint gp = doc.getGeoPoint("location");
        if (gp != null) {
            return gp.getLongitude();
        }
        return Double.NaN;
    }

    @Nullable
    private static Double objectToDouble(@Nullable Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble(((String) o).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static List<GeoPoint> spreadDuplicateCoordinates(List<double[]> rawLatLng) {
        List<GeoPoint> out = new ArrayList<>();
        Map<String, Integer> keyToCount = new HashMap<>();

        for (int i = 0; i < rawLatLng.size(); i++) {
            double lat = rawLatLng.get(i)[0];
            double lng = rawLatLng.get(i)[1];
            String key = String.format(Locale.US, "%.5f,%.5f", lat, lng);
            int idx = keyToCount.getOrDefault(key, 0);
            keyToCount.put(key, idx + 1);

            double dLat = DUPLICATE_OFFSET_STEP * idx * Math.cos(idx * 1.17);
            double dLng = DUPLICATE_OFFSET_STEP * idx * Math.sin(idx * 1.17);
            out.add(new GeoPoint(lat + dLat, lng + dLng));
        }
        return out;
    }

    private static BoundingBox boundingBoxWithMinimumSpan(List<GeoPoint> points,
                                                          double minLatDegrees,
                                                          double minLonDegrees) {
        BoundingBox box = BoundingBox.fromGeoPoints(points);
        double latSpan = box.getLatNorth() - box.getLatSouth();
        double lonSpan = box.getLonEast() - box.getLonWest();
        if (latSpan < minLatDegrees || lonSpan < minLonDegrees) {
            double latCenter = (box.getLatNorth() + box.getLatSouth()) / 2.0;
            double lonCenter = (box.getLonEast() + box.getLonWest()) / 2.0;
            double halfLat = Math.max(latSpan / 2.0, minLatDegrees / 2.0);
            double halfLon = Math.max(lonSpan / 2.0, minLonDegrees / 2.0);
            return new BoundingBox(
                    latCenter + halfLat,
                    lonCenter + halfLon,
                    latCenter - halfLat,
                    lonCenter - halfLon);
        }
        return box;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            scheduleReapplyFirestoreCamera();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        clearSavedFirestoreCamera();
        mapView = null;
        super.onDestroyView();
    }
}
