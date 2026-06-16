package com.chikchak.dto;

import com.chikchak.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "Phone verification token is required")
    private String firebaseIdToken;

    // Same validation as RegisterRequest.password so reset cannot be used as a
    // backdoor around the registration password rules. Keep these two in sync.
    @NotBlank(message = "Password is required")
    @Pattern(regexp = ValidationConstants.PASSWORD_REGEX, message = ValidationConstants.PASSWORD_MESSAGE)
    private String newPassword;
}
