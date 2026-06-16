package com.chikchak.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Optional reason body for admin actions (reject, suspend, cancel)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReasonRequest {
    @Size(max = 500, message = "Reason is too long")
    private String reason;
}
