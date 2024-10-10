package com.unipi.ppapakostas.autotrack.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.unipi.ppapakostas.autotrack.Database;
import com.unipi.ppapakostas.autotrack.LocationWorker;
import com.unipi.ppapakostas.autotrack.Login;
import com.unipi.ppapakostas.autotrack.R;
import com.unipi.ppapakostas.autotrack.service.TrackingService;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private RelativeLayout RLStartTracking, RL_RLCurrentSpeed, RL_Logout;
    private TextView tvCurrentSpeed;

    // Firebase authentication
    private FirebaseAuth auth;
    private FirebaseUser user;

    // Firebase database
    Database database;

    // Location
    public static final int LOCATION_CODE = 23;

    // Variables for tracking state
    private boolean isTracking = false;
    private Animation pulseAnimation;

    // Tracking Service
    private Intent trackingServiceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Firebase Authentication
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        database = Database.getInstance();
        if (user == null) {
            // Redirect to Login activity if the user is not logged in
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }

        // Initialize UI elements
        RLStartTracking = findViewById(R.id.RLStartTracking);
        RL_RLCurrentSpeed = findViewById(R.id.RL_RLCurrentSpeed);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        RL_Logout = findViewById(R.id.RL_Logout);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);

        // Logout button logic
        RL_Logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            stopTracking();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        // Bottom navigation logic
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_maps) {
                openGoogleMaps();
                return true;
            } else if (id == R.id.nav_recyclerview) {
                openRecyclerView();
                return true;
            }
            return false;
        });


        // Service Intent
        trackingServiceIntent = new Intent(this, TrackingService.class);

        // Start/Stop tracking based on user interaction
            RLStartTracking.setOnClickListener(view -> {
            if (!isTracking) {
                startTracking();
            } else {
                stopTracking();
            }
            isTracking = !isTracking;
        });

        // Ask for location permission
        askPermission();

        // Register broadcast receiver to receive updates from the TrackingService
        LocalBroadcastManager.getInstance(this).registerReceiver(speedUpdateReceiver,
                new IntentFilter(TrackingService.ACTION_LOCATION_BROADCAST));

        // Schedule the LocationWorker to collect data every 30 minutes
        schedulePeriodicLocationUpdates();
    }

    private void schedulePeriodicLocationUpdates() {
        PeriodicWorkRequest locationRequest = new PeriodicWorkRequest.Builder(LocationWorker.class, 30, TimeUnit.MINUTES)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(this).enqueue(locationRequest);

        Toast.makeText(this, "Periodic Location Updates Scheduled", Toast.LENGTH_SHORT).show();
    }

    // Function to request necessary permissions
    private void askPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_CODE);
        }
    }

    // Function to start tracking the user's location
    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askPermission();  // Ask for permission if not already granted
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(trackingServiceIntent);
        } else {
            startService(trackingServiceIntent);
        }
        RL_RLCurrentSpeed.setVisibility(TextView.VISIBLE);
        RLStartTracking.startAnimation(pulseAnimation);
        RLStartTracking.setBackground(ContextCompat.getDrawable(this, R.drawable.tracking_background));
        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show();
    }

        // Function to stop tracking the user's location
    private void stopTracking() {
        stopService(trackingServiceIntent);
        RL_RLCurrentSpeed.setVisibility(TextView.INVISIBLE);
        RLStartTracking.clearAnimation();
        RLStartTracking.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_main_button));
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show();
    }

    // Broadcast receiver to handle speed updates from the TrackingService
    private final BroadcastReceiver speedUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(TrackingService.CURRENT_SPEED)) {
                int speed = intent.getIntExtra(TrackingService.CURRENT_SPEED, 0);
                tvCurrentSpeed.setText("Speed: " + speed + " km/h");
            }
        }
    };

    // Function to handle opening the Google Maps activity
    private void openGoogleMaps() {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    // Function to handle opening the RecyclerView activity
    private void openRecyclerView() {
        Intent intent = new Intent(MainActivity.this, RecyclerViewActivity.class);
        startActivity(intent);
    }


}