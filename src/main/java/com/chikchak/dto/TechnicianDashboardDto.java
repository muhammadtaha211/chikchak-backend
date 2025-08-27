package com.chikchak.dto;

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
public class TechnicianDashboardDto {
    private List<BookingDto> pendingRequests;
    private List<BookingDto> activeJobs;
    private List<BookingDto> recentJobs;
    private BigDecimal todayEarnings;
    private BigDecimal thisWeekEarnings;
    private BigDecimal thisMonthEarnings;
    private BigDecimal totalEarnings;
    private Integer totalJobs;
    private BigDecimal averageRating;
    private Integer newRequests;
}
