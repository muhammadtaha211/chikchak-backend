package com.chikchak.model;

public enum BookingStatus {
    PENDING,           // Initial status when booking is created
    TECHNICIAN_NOTIFIED,  // Technician has been notified
    TIME_DISCUSSION,    // Technician requested a time change.
    ACCEPTED,          // Technician accepted the job
    SCHEDULED,         // Time has been agreed upon
    ON_THE_WAY,       // Technician is traveling to location
    ARRIVED,          // Technician has arrived (triggers payment release)
    IN_PROGRESS,      // Work is being performed
    ASSESSMENT_COMPLETE, // Diagnosis completed
    COMPLETED,        // Job fully completed
    CANCELLED,        // Cancelled by customer or technician
    DISPUTED          // Payment or service dispute
}
