package com.chikchak.dto;

import com.chikchak.model.ServiceType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTechnicianProfileRequest {
    @DecimalMin(value = "0.0", message = "Diagnosis fee cannot be negative")
    @DecimalMax(value = "100000.0", message = "Diagnosis fee is too high")
    private BigDecimal baseDiagnosisFee;

    @Size(max = 1000, message = "Bio is too long")
    private String bio;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 80, message = "Years of experience is unrealistic")
    private Integer yearsExperience;

    private Boolean isAvailable;

    @Min(value = 1, message = "Service radius must be at least 1 km")
    @Max(value = 200, message = "Service radius is too large")
    private Integer serviceRadius;

    @Size(max = 255, message = "Working hours is too long")
    private String workingHours;

    private ServiceType serviceType;
}
