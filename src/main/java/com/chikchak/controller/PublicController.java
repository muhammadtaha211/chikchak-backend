package com.chikchak.controller;

import com.chikchak.dto.ApiResponse;
import com.chikchak.model.ServiceType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "Public APIs that don't require authentication")
public class PublicController {

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
}
