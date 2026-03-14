package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingDto {
    private Long id;
    private Long bookingId;
    private String bookingNumber;
    private Long customerId;
    private String customerName;
    private Long technicianId;
    private Integer score;
    @Builder.Default
    private String comment = "";
    private LocalDateTime createdAt;
}
