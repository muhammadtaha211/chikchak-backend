package com.chikchak.model;

public enum ReportStatus {
    OPEN,       // awaiting admin review
    DISMISSED,  // reviewed, no action needed
    ACTIONED    // reviewed, content removed and/or user suspended
}
