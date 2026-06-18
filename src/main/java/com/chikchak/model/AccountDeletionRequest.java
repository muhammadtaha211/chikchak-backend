package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// A no-login account & data deletion request captured from the public web page
// (Google Play Data Safety requirement). The row is the durable source of truth:
// even if the operator notification email fails, the request is never lost and is
// reviewable via /api/admin/deletion-requests. requestedAt == BaseEntity.createdAt.
@Entity
@Table(name = "account_deletion_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDeletionRequest extends BaseEntity {

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeletionRequestStatus status = DeletionRequestStatus.PENDING;

    // Set when an admin processes or rejects the request
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by", length = 254)
    private String processedBy;
}
