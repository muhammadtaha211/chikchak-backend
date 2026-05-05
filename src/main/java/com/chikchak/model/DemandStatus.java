package com.chikchak.model;

public enum DemandStatus {
    PENDING,    // Demand registered, awaiting available technician
    NOTIFIED,   // Customer has been notified that a technician is available
    FULFILLED,  // Demand resulted in a booking
    EXPIRED     // Demand expired without fulfillment
}
