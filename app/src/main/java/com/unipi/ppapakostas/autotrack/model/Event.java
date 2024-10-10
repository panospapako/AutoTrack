package com.unipi.ppapakostas.autotrack.model;

public class Event {

    private final double latitude;
    private final double longitude;
    private final String timestamp;
    private final String eventType;
    private final float eventValue;

    public Event(double latitude, double longitude, String timestamp, String eventType, float eventValue) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.eventValue = eventValue;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public float getEventValue() {
        return eventValue;
    }
}

