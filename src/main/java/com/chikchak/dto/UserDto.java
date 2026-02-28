package com.chikchak.dto;

import com.chikchak.model.UserRole;
import com.chikchak.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;

    @Builder.Default
    private String email = "";

    @Builder.Default
    private String fullName = "";

    @Builder.Default
    private String phoneNumber = "";

    private UserRole role;
    private UserStatus status;

    @Builder.Default
    private String preferredLanguage = "he";

    @Builder.Default
    private String profileImageUrl = "";

    @Builder.Default
    private String address = "";

    @Builder.Default
    private String city = "";

    @Builder.Default
    private Double latitude = 0.0;

    @Builder.Default
    private Double longitude = 0.0;

    @Builder.Default
    private BigDecimal accountCredits = BigDecimal.ZERO;

    // REMOVED @JsonFormat — JacksonConfig handles DateTime globally now
    private LocalDateTime createdAt;
}