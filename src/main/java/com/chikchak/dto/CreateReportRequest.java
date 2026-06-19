package com.chikchak.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {

    @NotNull(message = "Reported user ID is required")
    private Long reportedUserId;

    // Optional: the specific message or rating being reported.
    private Long chatMessageId;
    private Long ratingId;

    @Size(max = 1000, message = "Reason is too long")
    private String reason;
}
