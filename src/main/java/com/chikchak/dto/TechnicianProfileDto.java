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
public class TechnicianProfileDto {
    private Long id;

    @Builder.Default
    private BigDecimal baseDiagnosisFee = BigDecimal.ZERO;

    @Builder.Default
    private String bio = "";

    @Builder.Default
    private Integer yearsExperience = 0;

    @Builder.Default
    private Boolean isVerified = false;

    @Builder.Default
    private Boolean isAvailable = false;

    @Builder.Default
    private Double averageRating = 5.0;

    @Builder.Default
    private Integer totalRatings = 0;

    @Builder.Default
    private Integer totalJobs = 0;

    @Builder.Default
    private Integer serviceRadius = 0;

    @Builder.Default
    private String workingHours = "";

    private ServiceCategoryDto serviceCategory;

    @Builder.Default
    private Double distanceFromCustomer = 0.0;

    private UserDto user;
}