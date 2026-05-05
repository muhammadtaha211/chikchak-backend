package com.chikchak.dto;

import com.chikchak.model.DemandStatus;
import com.chikchak.model.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianDemandRequestDto {
    private Long id;
    private Long userId;
    private ServiceType serviceType;
    private Double latitude;
    private Double longitude;
    private String address;
    private DemandStatus status;
    private LocalDateTime notifiedAt;
    private LocalDateTime createdAt;
}
