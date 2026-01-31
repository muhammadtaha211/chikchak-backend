package com.chikchak.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundChoiceRequest {
    @NotNull(message = "Refund method is required")
    private RefundMethod refundMethod; // CREDITS or BANK

    private String reason; // Optional cancellation reason
}