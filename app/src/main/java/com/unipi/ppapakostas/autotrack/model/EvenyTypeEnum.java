package com.unipi.ppapakostas.autotrack.model;

public enum EvenyTypeEnum {

    RAPID_ACCELERATION("Rapid Acceleration"),
    BRAKING("Braking"),
    SPEED_LIMIT_VIOLATION("Speed Limit Violation"),
    POTHOLE("Pothole");

    private final String value;

    EvenyTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
