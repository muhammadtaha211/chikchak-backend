package com.chikchak.controller;

import com.chikchak.dto.AccountDeletionRequestDto;
import com.chikchak.dto.ApiResponse;
import com.chikchak.model.ServiceType;
import com.chikchak.service.DataDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public APIs that don't require authentication")
public class PublicController {

    private final DataDeletionService dataDeletionService;

    @GetMapping("/services")
    @Operation(summary = "Get service types", description = "Returns the list of supported service type enum values")
    public ResponseEntity<ApiResponse<List<String>>> getServices() {
        List<String> services = Arrays.stream(ServiceType.values())
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "API health check endpoint")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("ChikChak API is running", "OK"));
    }

    @PostMapping("/account-deletion-request")
    @Operation(summary = "Request account & data deletion",
            description = "Public, no-login intake for account deletion requests (Google Play Data Safety). "
                    + "Always returns success and never reveals whether the email matches an account.")
    public ResponseEntity<ApiResponse<Void>> requestAccountDeletion(
            @Valid @RequestBody AccountDeletionRequestDto request) {
        dataDeletionService.submitDeletionRequest(request);
        return ResponseEntity.ok(ApiResponse.<Void>success(
                "Your deletion request has been received. We will process it within 30 days "
                        + "and email you a confirmation.", null));
    }
}
