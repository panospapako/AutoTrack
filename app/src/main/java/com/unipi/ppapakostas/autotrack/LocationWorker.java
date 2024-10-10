package com.unipi.ppapakostas.autotrack;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.unipi.ppapakostas.autotrack.service.TrackingService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class LocationWorker extends Worker {

    private static final String TAG = "LocationWorker";
    private static final long SERVICE_RUNNING_DURATION = 5; // 5 minutes
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Periodic Location Update Started");

        // Check permissions before starting the service
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure();
        }

        // Start the TrackingService to collect detailed location and sensor data
        startTrackingService();

        // Schedule the stop of the service after 5 minutes
        scheduler.schedule(this::stopTrackingService, SERVICE_RUNNING_DURATION, TimeUnit.MINUTES);

        return Result.success();
    }

    private void startTrackingService() {
        Context context = getApplicationContext();

        // Create an Intent to start the TrackingService
        Intent trackingServiceIntent = new Intent(context, TrackingService.class);

        // Check for Android Version and start the service accordingly
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.d(TAG, "Starting TrackingService as a Foreground Service from WorkManager");
            context.startForegroundService(trackingServiceIntent);
        } else {
            Log.d(TAG, "Starting TrackingService as a Background Service from WorkManager");
            context.startService(trackingServiceIntent);
        }
    }

    private void stopTrackingService() {
        Context context = getApplicationContext();

        // Create an Intent to stop the TrackingService
        Intent trackingServiceIntent = new Intent(context, TrackingService.class);
        Log.d(TAG, "Stopping TrackingService after 5 minutes");

        context.stopService(trackingServiceIntent);
    }
}
