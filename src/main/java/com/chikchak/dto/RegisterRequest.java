package com.chikchak.dto;

import com.chikchak.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phoneNumber;

    @NotNull(message = "User role is required")
    private UserRole role;

    private String address;
    private String city;
    private Double latitude;
    private Double longitude;

    private String preferredLanguage = "he";

    // Technician-specific fields
    private BigDecimal baseDiagnosisFee;
    private String bio;
    private Integer yearsExperience;
    private Set<Long> serviceIds;
}
