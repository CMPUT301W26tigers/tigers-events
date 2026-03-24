package com.example.eventsapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * US 02.02.02 — Organizer view: shows a map of where waitlist entrants joined from.
 *
 * Each entrant document that has a non-zero latitude/longitude stored is rendered
 * as a marker on an OpenStreetMap tile layer (OSMDroid, no API key required).
 * Markers show the entrant's name on tap.
 */
public class EntrantMapFragment extends Fragment {

    private static final double DEFAULT_ZOOM = 3.0;

    private MapView mapView;
    private TextView tvSubtitle;
    private String eventId;
    private String eventName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId", "");
            eventName = getArguments().getString("eventName", "Event");
        }

        // Use app-private storage for OSMDroid tile cache (no WRITE_EXTERNAL_STORAGE needed)
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(requireContext().getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(requireContext().getCacheDir());
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

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_entrant_map);
        toolbar.setTitle(eventName != null ? eventName + " — Entrant Map" : "Entrant Locations");
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tvSubtitle = view.findViewById(R.id.tv_map_subtitle);

        mapView = view.findViewById(R.id.map_view);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(new GeoPoint(20.0, 0.0));

        loadEntrantLocations();
    }

    /**
     * Queries Firestore for all entrants with stored GPS coordinates and places a
     * marker on the map for each one.
     */
    private void loadEntrantLocations() {
        if (eventId == null || eventId.isEmpty()) {
            tvSubtitle.setText("No event selected.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events").document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    List<GeoPoint> points = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");

                        if (lat == null || lng == null) continue;
                        if (lat == 0.0 && lng == 0.0) continue;

                        String name = doc.getString("name");
                        if (name == null || name.isEmpty()) name = "Unknown entrant";

                        GeoPoint point = new GeoPoint(lat, lng);
                        points.add(point);

                        Marker marker = new Marker(mapView);
                        marker.setPosition(point);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        marker.setTitle(name);
                        marker.setSnippet("Lat: " + String.format("%.4f", lat)
                                + "  Lng: " + String.format("%.4f", lng));
                        mapView.getOverlays().add(marker);
                    }

                    int count = points.size();
                    tvSubtitle.setText(count == 0
                            ? "No location data yet. Location is recorded when entrants join the waitlist."
                            : count + " entrant" + (count == 1 ? "" : "s") + " with location data");

                    if (!points.isEmpty()) {
                        zoomToFitMarkers(points);
                    }

                    mapView.invalidate();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvSubtitle.setText("Failed to load entrant locations.");
                });
    }

    /**
     * Zooms and pans the map so all marker points are visible.
     *
     * @param points list of GeoPoints to fit into view.
     */
    private void zoomToFitMarkers(List<GeoPoint> points) {
        if (points.size() == 1) {
            mapView.getController().setZoom(10.0);
            mapView.getController().setCenter(points.get(0));
            return;
        }

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;

        for (GeoPoint p : points) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLng = Math.min(minLng, p.getLongitude());
            maxLng = Math.max(maxLng, p.getLongitude());
        }

        // Add padding around the bounding box
        double pad = 1.0;
        BoundingBox box = new BoundingBox(maxLat + pad, maxLng + pad, minLat - pad, minLng - pad);
        mapView.zoomToBoundingBox(box, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) mapView.onDetach();
    }
}
