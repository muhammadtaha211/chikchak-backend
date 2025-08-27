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
public class AdminDashboardDto {
    private Long totalUsers;
    private Long totalTechnicians;
    private Long totalCustomers;
    private Long totalBookings;
    private Long activeBookings;
    private Long completedBookings;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private List<BookingDto> recentBookings;
    private List<UserDto> pendingTechnicians;
}
