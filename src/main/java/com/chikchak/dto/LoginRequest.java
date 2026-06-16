package com.chikchak.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Authentication DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 254, message = "Email is too long")
    private String email;

    // Login keeps the lenient minimum so existing accounts are never locked out;
    // the max only blocks oversized input. New/changed passwords use the stricter policy.
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;
}

