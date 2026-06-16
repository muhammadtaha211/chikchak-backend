package com.chikchak.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.chikchak.model.ServiceType;
import java.math.BigDecimal;

// Search and Filter DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianSearchRequest {
    private ServiceType serviceType;

    @DecimalMin(value = "-90.0", message = "Invalid latitude")
    @DecimalMax(value = "90.0", message = "Invalid latitude")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Invalid longitude")
    @DecimalMax(value = "180.0", message = "Invalid longitude")
    private Double longitude;

    @Min(value = 1, message = "Max distance must be at least 1 km")
    @Max(value = 200, message = "Max distance is too large")
    private Integer maxDistance;

    @DecimalMin(value = "0.0", message = "Max price cannot be negative")
    private BigDecimal maxPrice;

    @DecimalMin(value = "0.0", message = "Min rating must be between 0 and 5")
    @DecimalMax(value = "5.0", message = "Min rating must be between 0 and 5")
    private Double minRating;

    private Boolean emergencyOnly;

    // Pagination
    @Min(value = 0, message = "Page cannot be negative")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size is too large")
    private int size = 20;

    @Pattern(regexp = "^[a-zA-Z]{0,30}$", message = "Invalid sort field")
    private String sortBy = "rating";

    @Pattern(regexp = "^(?i)(asc|desc)?$", message = "Sort direction must be asc or desc")
    private String sortDirection = "desc";
}
