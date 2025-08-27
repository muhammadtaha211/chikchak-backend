package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianProfileDto {
    private Long id;
    private BigDecimal baseDiagnosisFee;
    private String bio;
    private Integer yearsExperience;
    private Boolean isVerified;
    private Boolean isAvailable;
    private BigDecimal averageRating;
    private Integer totalJobs;
    private Integer serviceRadius;
    private String workingHours;
    private Set<ServiceCategoryDto> services;
    private Double distanceFromCustomer; // Calculated field
}
