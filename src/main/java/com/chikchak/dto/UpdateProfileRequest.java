package com.chikchak.dto;

import com.chikchak.validation.ValidationConstants;
import jakarta.validation.constraints.*;
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
    @Size(max = 100, message = "Full name is too long")
    private String fullName;

    @Pattern(regexp = ValidationConstants.PHONE_REGEX, message = ValidationConstants.PHONE_MESSAGE)
    private String phoneNumber;

    @Size(max = 255, message = "Address is too long")
    private String address;

    @Size(max = 100, message = "City is too long")
    private String city;

    @DecimalMin(value = "-90.0", message = "Invalid latitude")
    @DecimalMax(value = "90.0", message = "Invalid latitude")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Invalid longitude")
    @DecimalMax(value = "180.0", message = "Invalid longitude")
    private Double longitude;

    @Pattern(regexp = ValidationConstants.LANGUAGE_REGEX, message = ValidationConstants.LANGUAGE_MESSAGE)
    private String preferredLanguage;
}
