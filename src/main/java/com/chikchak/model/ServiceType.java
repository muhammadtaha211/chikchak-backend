package com.chikchak.model;

public enum ServiceType {
    PLUMBING("אינסטלציה"),
    ELECTRICAL("חשמל"),
    AC_REPAIR("מיזוג אוויר"),
    MOVERS("הובלות"),
    CLEANING("ניקיון"),
    CARPENTRY("נגרות"),
    PAINTING("צביעה"),
    GARDENING("גינון");

    private final String hebrewName;

    ServiceType(String hebrewName) {
        this.hebrewName = hebrewName;
    }

    public String getHebrewName() {
        return hebrewName;
    }
}
