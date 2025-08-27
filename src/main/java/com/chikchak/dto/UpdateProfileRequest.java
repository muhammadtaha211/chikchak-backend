package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Profile Update DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String fullName;
    private String phoneNumber;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String preferredLanguage;
}
