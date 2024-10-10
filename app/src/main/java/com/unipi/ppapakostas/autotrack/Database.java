package com.unipi.ppapakostas.autotrack;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class Database {

    private static final String TAG = "Database";
    private static final String COLLECTION_NAME = "events";

    private FirebaseFirestore db;

    // Singleton instance
    private static Database databaseInstance;

    private Database() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized Database getInstance() {
        if (databaseInstance == null) {
            databaseInstance = new Database();
        }
        return databaseInstance;
    }

    /**
     * Inserts a new event into the Firebase Firestore with additional support for event types.
     *
     * @param lon         The longitude of the event
     * @param lat         The latitude of the event
     * @param timeStamp   The timestamp of the event
     * @param acceleration The recorded acceleration value
     * @param eventType   The type of event (e.g., "braking", "acceleration", "speed_limit", "pothole")
     */
    public void insert(double lon, double lat, String timeStamp, float acceleration, String eventType) {
        // Create a new event map to hold the data
        Map<String, Object> event = new HashMap<>();
        event.put("lon", lon);
        event.put("lat", lat);
        event.put("time", timeStamp);
        event.put("acceleration", acceleration);
        event.put("event_type", eventType);

        // Insert the event into the Firestore collection
        db.collection(COLLECTION_NAME)
                .add(event)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Event added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding event", e));
    }

    /**
     * Retrieves all events from the Firestore and passes the result to the callback.
     *
     * @param callback The callback to handle the retrieved events
     */
    public void getAllEvents(@NonNull final DataCallback callback) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            callback.onSuccess(querySnapshot);
                        } else {
                            callback.onFailure(new Exception("No events found."));
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }


    // Interface for callback to handle data
    public interface DataCallback {
        void onSuccess(QuerySnapshot querySnapshot);
        void onFailure(Exception e);
    }
}
