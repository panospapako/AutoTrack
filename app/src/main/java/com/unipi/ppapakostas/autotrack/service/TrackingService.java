package com.unipi.ppapakostas.autotrack.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.unipi.ppapakostas.autotrack.Database;
import com.unipi.ppapakostas.autotrack.NotificationReceiver;
import com.unipi.ppapakostas.autotrack.R;
import com.unipi.ppapakostas.autotrack.model.EvenyTypeEnum;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TrackingService extends Service implements SensorEventListener {


    private static final String CHANNEL_ID = "TrackingServiceChannel";
    public static final String ACTION_LOCATION_BROADCAST = "TrackingServiceLocationBroadcast";

    // Variables for tracking state and speed
    private float lastSpeed = 0f;
    private long lastTime = 0;
    private String eventType;
    private double lon, lat;
    private String timeStamp;
    public static final String CURRENT_SPEED = "Speed";
    private static final int MIN_SPEED_THRESHOLD = 5;

    // Firebase database
    private Database database;

    // Location and tracking elements
    public static final int REQUEST_CODE = 10;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    // Accelerometer variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastVerticalAcceleration = 0f;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize location services and sensors
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        database = Database.getInstance();

        // Define location request parameters
        locationRequest = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        // Location callback to handle location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    handleLocationUpdate(location);
                }
            }
        };

        startForegroundService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        startLocationUpdates();
        return START_STICKY;
    }

    // Start location updates
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // Handle location update
    private void handleLocationUpdate(Location location) {
        lon = location.getLongitude();
        lat = location.getLatitude();
        float currentSpeed = location.getSpeed(); // speed in m/s
        int roundedSpeed = (int) Math.round(currentSpeed * 3.6); // Convert to km/h
        long currentTime = System.currentTimeMillis();

        // Update speed in the UI via broadcast
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(CURRENT_SPEED, roundedSpeed);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (lastTime != 0) {
            // Calculate acceleration
            float speedDifference = currentSpeed - lastSpeed;
            long timeDifference = currentTime - lastTime; // in milliseconds
            float timeDifferenceSeconds = timeDifference / 1000f;
            float acceleration = speedDifference / timeDifferenceSeconds;

            // Detect rapid acceleration (above 2.5 m/s²)
            if (acceleration > 2.5) {
                eventType = EvenyTypeEnum.RAPID_ACCELERATION.getValue();
                Log.d(eventType, "Detected rapid acceleration with acceleration: " + acceleration);
                saveEvent(location, eventType, acceleration);
                setNotification(lon, lat, timeStamp, eventType);
            }

            // Detect braking when horizontal acceleration is below -2 m/s²
            if (acceleration < -2) {
                eventType = EvenyTypeEnum.BRAKING.getValue();
                Log.d(eventType, "Braking detected with acceleration: " + acceleration);
                saveEvent(location, eventType, acceleration);
                setNotification(lon, lat, timeStamp, eventType);
            }

            // Detect speed limit exceed (speed > 30 km/h)
            if (roundedSpeed > 30) {
                eventType = EvenyTypeEnum.SPEED_LIMIT_VIOLATION.getValue();
                Log.d(eventType, "Speed limit exceeded with speed: " + roundedSpeed);
                saveEvent(location, eventType, acceleration);
                setNotification(lon, lat, timeStamp, eventType);
            }

            // Detect potholes based on vertical acceleration and speed
            if (roundedSpeed >= MIN_SPEED_THRESHOLD && Math.abs(lastVerticalAcceleration) > 3.0) {
                // Detecting sudden vertical changes
                eventType = EvenyTypeEnum.POTHOLE.getValue();
                Log.d(eventType, "Pothole detected with vertical acceleration: " + lastVerticalAcceleration);
                saveEvent(location, eventType, lastVerticalAcceleration);
                setNotification(lon, lat, timeStamp, eventType);
            }
        }

        lastSpeed = currentSpeed;
        lastTime = currentTime;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            lastVerticalAcceleration = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // Save event to database
    private void saveEvent(Location location, String eventType, float eventValue) {
        timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        database.insert(location.getLongitude(), location.getLatitude(), timeStamp, eventValue, eventType);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        sensorManager.unregisterListener(this);
    }

    /**
     * Sets a notification to alert the user when event is detected.
     *
     * @param lon       The longitude of the event
     * @param lat       The latitude of the event
     * @param timestamp The timestamp of the event
     * @param eventType The type of the event
     */
    @SuppressLint("ScheduleExactAlarm")
    private void setNotification(Double lon, Double lat, String timestamp, String eventType){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 3);

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("Lon", lon);
        intent.putExtra("Lat", lat);
        intent.putExtra("Time", timestamp);
        intent.putExtra("EventType", eventType);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Tracking Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking Service")
                .setContentText("Tracking your location and movement")
                .setSmallIcon(R.drawable.ic_icon_location)
                .build();

        startForeground(1, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
