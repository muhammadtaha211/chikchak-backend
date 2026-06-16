package com.chikchak.validation;

/**
 * Shared Bean Validation constants. Kept here so the password policy is defined once
 * and reused across register / reset / change-password DTOs (annotation values must be
 * compile-time constants).
 */
public final class ValidationConstants {

    private ValidationConstants() {
    }

    // At least 8 chars (max 72 — BCrypt's effective limit), including a letter and a digit.
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be 8-72 characters and include at least one letter and one number";

    // Lenient phone format: optional leading +, digits, spaces and dashes.
    // The outer (...)? also permits an empty string so optional phone fields aren't rejected.
    public static final String PHONE_REGEX = "^([+]?[0-9\\s-]{6,20})?$";
    public static final String PHONE_MESSAGE = "Phone number format is invalid";

    // Supported UI languages; empty string allowed so an unset field passes.
    public static final String LANGUAGE_REGEX = "^(he|ar|en)?$";
    public static final String LANGUAGE_MESSAGE = "Unsupported language";
}
