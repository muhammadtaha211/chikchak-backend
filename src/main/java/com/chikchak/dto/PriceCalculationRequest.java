package com.chikchak.dto;

import com.chikchak.model.ServiceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCalculationRequest {
    @NotNull(message = "Technician ID is required")
    private Long technicianId;

    @NotNull(message = "Customer latitude is required")
    private Double customerLatitude;

    @NotNull(message = "Customer longitude is required")
    private Double customerLongitude;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Builder.Default
    private Boolean useCredits = false;
}