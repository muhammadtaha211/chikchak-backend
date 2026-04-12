package com.chikchak.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveTechnicianJobPricingRequest {

    @Valid
    @NotNull
    private List<JobPricingItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobPricingItem {
        @NotNull
        private Long jobTypeId;

        @NotNull
        private BigDecimal minPrice;

        @NotNull
        private BigDecimal maxPrice;

        private Boolean isOffered = true;
    }
}
