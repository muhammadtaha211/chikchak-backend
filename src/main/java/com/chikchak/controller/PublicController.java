package com.chikchak.controller;

import com.chikchak.dto.ApiResponse;
import com.chikchak.dto.ServiceCategoryDto;
import com.chikchak.service.ServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public APIs that don't require authentication")
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class PublicController {

    private final ServiceCategoryService serviceCategoryService;

    @GetMapping("/services")
    @Operation(summary = "Get service categories", description = "Get all active service categories")
    public ResponseEntity<ApiResponse<List<ServiceCategoryDto>>> getServices() {
        List<ServiceCategoryDto> services = serviceCategoryService.getActiveServices();
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "API health check endpoint")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("ChikChak API is running", "OK"));
    }
}
