package com.chikchak.dto;

import com.chikchak.model.ServiceType;
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
    private BigDecimal baseDiagnosisFee;
    private String bio;
    private Integer yearsExperience;
    private Boolean isAvailable;
    private Integer serviceRadius;
    private String workingHours;
    private ServiceType serviceType;
}
