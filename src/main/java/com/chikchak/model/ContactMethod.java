package com.chikchak.model;

public enum ContactMethod {
    EMAIL("Email Address"),
    PHONE("Phone Call"),
    IN_APP("In-App Message");

    private final String displayName;

    ContactMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}