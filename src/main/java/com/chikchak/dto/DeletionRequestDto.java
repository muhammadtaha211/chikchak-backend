package com.chikchak.dto;

import com.chikchak.model.DeletionRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Admin-facing view of a stored account-deletion request.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletionRequestDto {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String reason;
    private DeletionRequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private String processedBy;
}
