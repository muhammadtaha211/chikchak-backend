package com.chikchak.service;

import com.chikchak.model.ServiceType;

// Hebrew display names for service types, used in system messages and push notifications.
// Kept out of the ServiceType enum — the enum names are an English API contract the frontend relies on.
public final class ServiceTypeTranslator {

    private ServiceTypeTranslator() {
    }

    public static String toHebrew(ServiceType serviceType) {
        if (serviceType == null) {
            return "";
        }
        return switch (serviceType) {
            case PLUMBING -> "אינסטלציה";
            case ELECTRICAL -> "חשמל";
            case AC_REPAIR -> "מיזוג אוויר";
            case MOVERS -> "הובלות";
            case CLEANING -> "ניקיון";
            case CARPENTRY -> "נגרות";
            case PAINTING -> "צביעה";
            case GARDENING -> "גינון";
        };
    }
}
