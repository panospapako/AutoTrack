package com.unipi.ppapakostas.autotrack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.unipi.ppapakostas.autotrack.service.TrackingService;

public class NotificationReceiver extends BroadcastReceiver {
    public String TAG = "PowerManager";
    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        // Get the data that you passed
        Double lon = intent.getDoubleExtra("Lon", 0);
        Double lat = intent.getDoubleExtra("Lat", 0);
        String timeStamp = intent.getStringExtra("Time");
        String eventType = intent.getStringExtra("EventType");

        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        wakeLock.acquire();
        setNotification(context, lon, lat, timeStamp, eventType);
        wakeLock.release();
    }

    private void setNotification(Context context, Double lon, Double lat, String timeStamp, String eventType) {
        NotificationHelper notificationHelper = new NotificationHelper(context, lon, lat, timeStamp, eventType);
        NotificationCompat.Builder builder = notificationHelper.getNotification();
        notificationHelper.getManager().notify(TrackingService.REQUEST_CODE, builder.build());
    }

    static class NotificationHelper {
        private final String channelID = "channelID";
        private NotificationManager mManager;
        private final Double lon;
        private final Double lat;
        private final String timeStamp;
        private final String eventType;
        private final Context context;

        public NotificationHelper(Context context, Double lon, Double lat, String timeStamp, String eventType) {
            this.context = context;
            this.lon = lon;
            this.lat = lat;
            this.timeStamp = timeStamp;
            this.eventType = eventType;

            createChannel();
        }
        private void createChannel() {
            String channelName = "General";

            NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_HIGH);
            getManager().createNotificationChannel(channel);
        }
        public NotificationManager getManager() {
            if (mManager == null)
                mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            return mManager;
        }

        public NotificationCompat.Builder getNotification() {
            return new NotificationCompat.Builder(context, channelID)
                    .setContentTitle("New Event Detected")
                    .setContentText("Lon " + lon + " , " + "Lat " + lat + " , " + "Time " + timeStamp + " , " + eventType)
                    .setSmallIcon(R.drawable.ic_icon_car_black);
        }
    }
}
