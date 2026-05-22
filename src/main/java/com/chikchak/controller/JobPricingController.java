package com.chikchak.controller;

import com.chikchak.dto.ApiResponse;
import com.chikchak.dto.JobTypeDto;
import com.chikchak.dto.SaveTechnicianJobPricingRequest;
import com.chikchak.dto.TechnicianJobPricingDto;
import com.chikchak.model.ServiceType;
import com.chikchak.service.JobPricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Job Pricing", description = "Job types and technician pricing APIs")
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class JobPricingController {

    private final JobPricingService jobPricingService;

    @GetMapping("/api/public/job-types/{serviceType}")
    @Operation(summary = "Get job types by service type", description = "Returns all predefined job types for a service type")
    public ResponseEntity<ApiResponse<List<JobTypeDto>>> getJobTypesByServiceType(
            @PathVariable ServiceType serviceType) {
        List<JobTypeDto> jobTypes = jobPricingService.getJobTypesByServiceType(serviceType);
        return ResponseEntity.ok(ApiResponse.success(jobTypes));
    }

    // Dormant: per-technician pricing replaced by platform-set JobType.minPrice/maxPrice.
    @Deprecated
    @GetMapping("/api/public/technician-pricing/{technicianId}")
    @Operation(summary = "Get technician pricing", description = "Returns a technician's offered jobs with price ranges")
    public ResponseEntity<ApiResponse<List<TechnicianJobPricingDto>>> getTechnicianPricing(
            @PathVariable Long technicianId) {
        List<TechnicianJobPricingDto> pricing = jobPricingService.getOfferedTechnicianPricing(technicianId);
        return ResponseEntity.ok(ApiResponse.success(pricing));
    }

    // Dormant: per-technician pricing replaced by platform-set JobType.minPrice/maxPrice.
    @Deprecated
    @GetMapping("/api/technician/job-pricing")
    @Operation(summary = "Get my job pricing", description = "Returns all the technician's jobs with price ranges, including non-offered")
    public ResponseEntity<ApiResponse<List<TechnicianJobPricingDto>>> getMyPricing() {
        List<TechnicianJobPricingDto> pricing = jobPricingService.getMyPricing();
        return ResponseEntity.ok(ApiResponse.success(pricing));
    }

    // Dormant: per-technician pricing replaced by platform-set JobType.minPrice/maxPrice.
    @Deprecated
    @PutMapping("/api/technician/job-pricing")
    @Operation(summary = "Save technician job pricing", description = "Save or update the technician's offered jobs with price ranges")
    public ResponseEntity<ApiResponse<List<TechnicianJobPricingDto>>> saveTechnicianPricing(
            @Valid @RequestBody SaveTechnicianJobPricingRequest request) {
        List<TechnicianJobPricingDto> pricing = jobPricingService.saveTechnicianPricing(request);
        return ResponseEntity.ok(ApiResponse.success("Job pricing saved successfully", pricing));
    }
}
