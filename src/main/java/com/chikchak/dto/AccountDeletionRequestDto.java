package com.chikchak.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Public, no-login body for POST /api/public/account-deletion-request.
// Only the email is required (we look the account up by it); the rest helps the
// operator verify the request. `website` is a honeypot — see DataDeletionService.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDeletionRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @Size(max = 100, message = "Full name is too long")
    private String fullName;

    @Size(max = 30, message = "Phone number is too long")
    private String phoneNumber;

    @Size(max = 1000, message = "Reason is too long")
    private String reason;

    // Honeypot: hidden in the page and never filled by a human. Bots fill every
    // field, so a non-empty value marks the submission as spam.
    @Size(max = 200, message = "Invalid submission")
    private String website;
}
