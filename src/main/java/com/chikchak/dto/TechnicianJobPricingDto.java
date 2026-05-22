package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Dormant: platform pricing now lives on JobTypeDto.minPrice/maxPrice.
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianJobPricingDto {
    private Long id;
    private Long jobTypeId;
    @Builder.Default
    private String jobTypeName = "";
    @Builder.Default
    private String jobTypeNameHe = "";
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    @Builder.Default
    private Boolean isOffered = true;
}
