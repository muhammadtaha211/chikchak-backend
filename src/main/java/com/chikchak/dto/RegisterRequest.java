package com.chikchak.dto;

import com.chikchak.model.ServiceType;
import com.chikchak.model.UserRole;
import com.chikchak.validation.ValidationConstants;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = ValidationConstants.PASSWORD_REGEX, message = ValidationConstants.PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name is too long")
    private String fullName;

    @Pattern(regexp = ValidationConstants.PHONE_REGEX, message = ValidationConstants.PHONE_MESSAGE)
    private String phoneNumber;

    @NotNull(message = "User role is required")
    private UserRole role;

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
    private String preferredLanguage = "he";

    // Technician-specific fields
    @DecimalMin(value = "0.0", message = "Diagnosis fee cannot be negative")
    @DecimalMax(value = "100000.0", message = "Diagnosis fee is too high")
    private BigDecimal baseDiagnosisFee;

    @Size(max = 1000, message = "Bio is too long")
    private String bio;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 80, message = "Years of experience is unrealistic")
    private Integer yearsExperience;

    private ServiceType serviceType;
}
