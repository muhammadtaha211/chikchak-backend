package com.chikchak.controller;

import com.chikchak.dto.*;
import com.chikchak.service.AuthService;
import com.chikchak.service.BookingService;
import com.chikchak.service.TechnicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer-specific APIs")
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class CustomerController {

    private final BookingService bookingService;
    private final TechnicianService technicianService;
    private final AuthService authService;

    @GetMapping("/bookings")
    @Operation(summary = "Get customer bookings", description = "Get all bookings for current customer (all statuses)")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Long customerId = authService.getCurrentUserEntity().getId();

        List<BookingDto> bookings = bookingService.getCustomerBookings(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PostMapping("/search-technicians")
    @Operation(summary = "Search technicians", description = "Search for available technicians")
    public ResponseEntity<ApiResponse<TechnicianSearchResponse>> searchTechnicians(
            @Valid @RequestBody TechnicianSearchRequest request) {
        TechnicianSearchResponse response = technicianService.searchTechnicians(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Customer dashboard", description = "Get customer dashboard data")
    public ResponseEntity<ApiResponse<CustomerDashboardDto>> getDashboard() {
        CustomerDashboardDto dashboard = bookingService.getCustomerDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
