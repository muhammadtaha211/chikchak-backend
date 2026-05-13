package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianBadgeCountsDto {
    private Integer newRequestsCount;
    private Integer confirmedBookingsCount;
    private Integer timeDiscussionCount;
    private Integer scheduleTotalCount;
}
