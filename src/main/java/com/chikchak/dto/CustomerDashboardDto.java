package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// Dashboard DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDashboardDto {
    private List<BookingDto> activeBookings;
    private List<BookingDto> recentBookings;
    private BigDecimal accountCredits;
    private Integer totalBookings;
    private Integer completedBookings;
}
