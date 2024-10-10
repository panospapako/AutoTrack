package com.unipi.ppapakostas.autotrack.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.unipi.ppapakostas.autotrack.Database;
import com.unipi.ppapakostas.autotrack.R;
import com.unipi.ppapakostas.autotrack.model.EvenyTypeEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Database database;
    private ChipGroup filterChipGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        database = Database.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Home");

        filterChipGroup = findViewById(R.id.filter_chip_group);

        // Setup map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Setup filter chips listener for multiple selection
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            List<String> selectedFilters = getSelectedFilters(checkedIds);
            Log.d("FILTER", "Selected Filters: " + selectedFilters);

            if (mMap != null) {
                mMap.clear();
                addEventMarkers(selectedFilters);
            }
        });
    }

    private List<String> getSelectedFilters(List<Integer> checkedIds) {
        List<String> selectedFilters = new ArrayList<>();

        for (int checkedId : checkedIds) {
            if (checkedId == R.id.chip_braking) {
                selectedFilters.add(EvenyTypeEnum.BRAKING.getValue());
            } else if (checkedId == R.id.chip_acceleration) {
                selectedFilters.add(EvenyTypeEnum.RAPID_ACCELERATION.getValue());
            } else if (checkedId == R.id.chip_speed_limit) {
                selectedFilters.add(EvenyTypeEnum.SPEED_LIMIT_VIOLATION.getValue());
            } else if (checkedId == R.id.chip_pothole) {
                selectedFilters.add(EvenyTypeEnum.POTHOLE.getValue());
            }
        }

        return selectedFilters;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
            filterChipGroup.getChildAt(i).setSelected(true);
            ((Chip) filterChipGroup.getChildAt(i)).setChecked(true);
        }

        addEventMarkers(new ArrayList<>(Collections.singletonList("all")));// Display all markers by default
    }

    private void addEventMarkers(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return; // Do not display any markers if no filters are selected
        }

        database.getAllEvents(new Database.DataCallback() {
            @Override
            public void onSuccess(QuerySnapshot querySnapshot) {
                if (querySnapshot.isEmpty()) {
                    Toast.makeText(MapsActivity.this, "No events found", Toast.LENGTH_SHORT).show();
                    return;
                }

                LatLng firstLocation = null;

                for (QueryDocumentSnapshot document : querySnapshot) {
                    double lat = document.getDouble("lat");
                    double lon = document.getDouble("lon");
                    String timeStamp = document.getString("time");
                    String eventType = document.getString("event_type");

                    // Add markers if the event type matches one of the selected filters
                    if (filters.contains("all") || filters.contains(eventType)) {
                        LatLng location = new LatLng(lat, lon);
                        addMarker(location, timeStamp, eventType);

                        if (firstLocation == null) {
                            firstLocation = location;
                        }
                    }
                }

                if (firstLocation != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 15));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.d("MAP", "Error getting events: ", e);
            }
        });
    }

    private void addMarker(LatLng location, String timeStamp, String eventType) {
        // Handle the case where eventType is null
        if (eventType == null) {
            eventType = "default"; // Assign a default type to handle missing eventType
        }

        // Get marker color based on eventType
        float markerColor = getMarkerColor(eventType);

        // Add the marker with the specific color based on eventType
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(timeStamp)
                .snippet("Event: " + eventType)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
    }

    private float getMarkerColor(String eventType) {
        if (eventType == null) {
            return BitmapDescriptorFactory.HUE_MAGENTA; // Return a default color if eventType is null
        }

        if (eventType.equals(EvenyTypeEnum.BRAKING.getValue())) {
            return BitmapDescriptorFactory.HUE_RED;
        } else if (eventType.equals(EvenyTypeEnum.RAPID_ACCELERATION.getValue())) {
            return BitmapDescriptorFactory.HUE_GREEN;
        } else if (eventType.equals(EvenyTypeEnum.SPEED_LIMIT_VIOLATION.getValue())) {
            return BitmapDescriptorFactory.HUE_YELLOW;
        } else if (eventType.equals(EvenyTypeEnum.POTHOLE.getValue())) {
            return BitmapDescriptorFactory.HUE_ORANGE;
        } else {
            return BitmapDescriptorFactory.HUE_BLUE;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}