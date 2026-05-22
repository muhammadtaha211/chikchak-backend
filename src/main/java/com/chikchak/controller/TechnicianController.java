package com.chikchak.controller;

import com.chikchak.dto.*;
import com.chikchak.service.BookingService;
import com.chikchak.service.TechnicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/technician")
@RequiredArgsConstructor
@Tag(name = "Technician", description = "Technician-specific APIs")
public class TechnicianController {

    private final BookingService bookingService;
    private final TechnicianService technicianService;

    @GetMapping("/bookings")
    @Operation(summary = "Get technician bookings", description = "Get all bookings for current technician")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        // Implementation would get current technician ID
        List<BookingDto> bookings = technicianService.getTechnicianBookings(pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/pending-requests")
    @Operation(summary = "Get pending requests", description = "Get pending booking requests")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getPendingRequests() {
        List<BookingDto> bookings = bookingService.getPendingBookings();
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/bookings/time-discussion")
    @Operation(summary = "Get time-discussion bookings",
            description = "Get bookings where technician has proposed a new time and is awaiting customer response (status TIME_DISCUSSION)")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getTimeDiscussionBookings() {
        List<BookingDto> bookings = bookingService.getTimeDiscussionBookingsForTechnician();
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/bookings/history")
    @Operation(summary = "Get technician job history",
            description = "Get completed and cancelled bookings for the current authenticated technician")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getJobHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        List<BookingDto> bookings = bookingService.getCurrentTechnicianJobHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/badge-counts")
    @Operation(summary = "Get badge counts",
            description = "Get counts for navbar and tab badges (new requests, confirmed bookings, time-discussion bookings, and the schedule tab total)")
    public ResponseEntity<ApiResponse<TechnicianBadgeCountsDto>> getBadgeCounts() {
        TechnicianBadgeCountsDto counts = bookingService.getBadgeCounts();
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get technician profile", description = "Get current technician's profile details")
    public ResponseEntity<ApiResponse<TechnicianProfileDto>> getProfile() {
        TechnicianProfileDto profile = technicianService.getCurrentTechnicianProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update technician profile", description = "Update technician-specific profile details")
    public ResponseEntity<ApiResponse<TechnicianProfileDto>> updateProfile(
            @Valid @RequestBody UpdateTechnicianProfileRequest request) {
        TechnicianProfileDto profile = technicianService.updateTechnicianProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }

    @PutMapping("/availability")
    @Operation(summary = "Update availability", description = "Update technician availability status")
    public ResponseEntity<ApiResponse<String>> updateAvailability(@RequestParam Boolean available) {
        technicianService.updateAvailability(available);
        return ResponseEntity.ok(ApiResponse.success("Availability updated successfully", null));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Technician dashboard", description = "Get technician dashboard data")
    public ResponseEntity<ApiResponse<TechnicianDashboardDto>> getDashboard() {
        TechnicianDashboardDto dashboard = technicianService.getTechnicianDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
