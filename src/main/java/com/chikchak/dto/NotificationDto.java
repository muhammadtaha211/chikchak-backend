package com.chikchak.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private String type;
    private Long bookingId;
    private String message;
    private String timestamp;
    private Map<String, Object> data;
}