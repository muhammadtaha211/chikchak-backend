package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingCalculationDto {
    private BigDecimal diagnosisFee;
    private BigDecimal distanceKm;
    private BigDecimal distanceFee;
    private BigDecimal serviceFee;
    private BigDecimal totalAmount;
    private BigDecimal creditsApplied;
    private BigDecimal finalTotal;
}