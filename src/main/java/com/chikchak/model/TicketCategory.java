package com.chikchak.model;

public enum TicketCategory {
    BILLING("Billing Question"),
    TECHNICAL("Technical Issue"),
    ACCOUNT("Account Problem"),
    SERVICE("Service Complaint"),
    BOOKING("Booking Issue"),
    PAYMENT("Payment Problem"),
    OTHER("Other");

    private final String displayName;

    TicketCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}