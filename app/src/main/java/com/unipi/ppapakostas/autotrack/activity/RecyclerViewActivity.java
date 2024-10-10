package com.unipi.ppapakostas.autotrack.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.unipi.ppapakostas.autotrack.Database;
import com.unipi.ppapakostas.autotrack.R;
import com.unipi.ppapakostas.autotrack.adapter.EventAdapter;
import com.unipi.ppapakostas.autotrack.model.Event;
import com.unipi.ppapakostas.autotrack.model.EvenyTypeEnum;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private Database database;
    private List<Event> events = new ArrayList<>();
    private ChipGroup filterChipGroup; // ChipGroup for filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        filterChipGroup = findViewById(R.id.filter_chip_group_recycler);

        database = Database.getInstance();

        adapter = new EventAdapter(events);
        recyclerView.setAdapter(adapter);

        Toolbar toolbar = findViewById(R.id.recyclerToolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Home");

        // Set up the ChipGroup listener for filtering
        setupChipGroupListener();

        // Select all filters by default
        selectAllFilters();

        // Fetch data asynchronously and update adapter when ready
        getBrakingPointsFromDatabase();

    }

    // Method to select all filters by default
    private void selectAllFilters() {
        for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) filterChipGroup.getChildAt(i);
            chip.setChecked(true);
        }
    }

    private void getBrakingPointsFromDatabase() {
        database.getAllEvents(new Database.DataCallback() {
            @Override
            public void onSuccess(QuerySnapshot querySnapshot) {
                events.clear();  // Clear the list to prevent duplicates

                // Loop through each document returned from Firestore
                for (QueryDocumentSnapshot document : querySnapshot) {
                    double lat = document.getDouble("lat");
                    double lon = document.getDouble("lon");
                    String timestamp = document.getString("time");
                    String eventType = document.getString("event_type");  // Get the event type
                    double eventValue = document.contains("acceleration") ? document.getDouble("acceleration") : 0.0;

                    // Add the event to the list
                    events.add(new Event(lat, lon, timestamp, eventType, (float) eventValue));
                }

                // Notify the adapter that the data has changed
                adapter.updateData(new ArrayList<>(events));
            }

            @Override
            public void onFailure(Exception e) {
                Log.d("FIRESTORE", "Error retrieving braking points: ", e);
                Toast.makeText(RecyclerViewActivity.this, "Failed to load braking points", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupChipGroupListener() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            List<String> selectedFilters = getSelectedFilters(checkedIds);

            // If no filter is selected, show nothing
            if (selectedFilters.isEmpty()) {
                adapter.updateData(new ArrayList<>()); // Empty the list
                return;
            }

            // Filter events based on the selected filters
            List<Event> filteredEvents = new ArrayList<>();
            for (Event event : events) {
                if (selectedFilters.contains("all") || selectedFilters.contains(event.getEventType())) {
                    filteredEvents.add(event);
                }
            }

            adapter.updateData(filteredEvents); // Update adapter with filtered data
        });
    }

    private List<String> getSelectedFilters(List<Integer> checkedIds) {
        List<String> selectedFilters = new ArrayList<>();

        // Add event types based on the selected chips
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
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}