package com.chikchak.dto;

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
    private String reason;
}
