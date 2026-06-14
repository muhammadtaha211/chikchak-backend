package com.chikchak.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Self-service account deletion — password re-confirmation guards against
// an unattended/stolen session permanently deleting the account
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {
    @NotBlank(message = "Password is required to delete your account")
    private String password;
}
