package com.chikchak.dto;

import com.chikchak.model.UserRole;
import com.chikchak.model.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// User DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private String preferredLanguage;
    private String profileImageUrl;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private BigDecimal accountCredits;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
}


