package com.chikchak.model;

// Lifecycle of an account-deletion request submitted through the public web form.
public enum DeletionRequestStatus {
    PENDING,    // awaiting operator review
    PROCESSED,  // account anonymized/deleted
    REJECTED    // dismissed (e.g. spam, or no matching account)
}
