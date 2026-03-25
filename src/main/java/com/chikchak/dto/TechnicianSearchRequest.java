package com.chikchak.dto;

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
    private Double latitude;
    private Double longitude;
    private Integer maxDistance;
    private BigDecimal maxPrice;
    private Double minRating;
    private Boolean emergencyOnly;

    // Pagination
    private int page = 0;
    private int size = 20;
    private String sortBy = "rating";
    private String sortDirection = "desc";
}
