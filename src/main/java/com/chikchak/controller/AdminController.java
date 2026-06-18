package com.chikchak.controller;

import com.chikchak.dto.*;
import com.chikchak.model.BookingStatus;
import com.chikchak.model.DeletionRequestStatus;
import com.chikchak.model.UserRole;
import com.chikchak.model.UserStatus;
import com.chikchak.service.AdminService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// All /api/admin/** endpoints require ROLE_ADMIN (enforced in SecurityConfig)
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs for the external admin website")
public class AdminController {

    private final AdminService adminService;

    // ─── Technician approval ─────────────────────────────────────────────

    @GetMapping("/technicians/pending")
    @Operation(summary = "Pending technicians", description = "List unverified technicians awaiting approval (oldest first)")
    public ResponseEntity<ApiResponse<List<TechnicianProfileDto>>> getPendingTechnicians(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TechnicianProfileDto> technicians = adminService.getPendingTechnicians(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(technicians));
    }

    @PostMapping("/technicians/{id}/approve")
    @Operation(summary = "Approve technician", description = "Set the technician's isVerified to true")
    public ResponseEntity<ApiResponse<TechnicianProfileDto>> approveTechnician(@PathVariable Long id) {
        TechnicianProfileDto technician = adminService.approveTechnician(id);
        return ResponseEntity.ok(ApiResponse.success("Technician approved", technician));
    }

    @PostMapping("/technicians/{id}/reject")
    @Operation(summary = "Reject technician", description = "Leave isVerified false; the optional reason is recorded in the audit log")
    public ResponseEntity<ApiResponse<TechnicianProfileDto>> rejectTechnician(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminReasonRequest request) {
        TechnicianProfileDto technician = adminService.rejectTechnician(id, reasonOf(request));
        return ResponseEntity.ok(ApiResponse.success("Technician rejected", technician));
    }

    @GetMapping("/technicians")
    @Operation(summary = "List technicians", description = "List/search all technicians (verified and not), paginated")
    public ResponseEntity<ApiResponse<List<TechnicianProfileDto>>> getTechnicians(
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TechnicianProfileDto> technicians = adminService.getAllTechnicians(verified, search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(technicians));
    }

    @GetMapping("/technicians/{id}")
    @Operation(summary = "Technician detail", description = "Full technician profile detail")
    public ResponseEntity<ApiResponse<TechnicianProfileDto>> getTechnician(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTechnicianDetail(id)));
    }

    // ─── User management ─────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "List users", description = "List/search users, paginated, filterable by role and status")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<UserDto> users = adminService.getUsers(role, status, search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "User detail", description = "Full user detail")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserDetail(id)));
    }

    @PostMapping("/users/{id}/suspend")
    @Operation(summary = "Suspend user", description = "Set user status to SUSPENDED; the user can no longer authenticate")
    public ResponseEntity<ApiResponse<UserDto>> suspendUser(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminReasonRequest request) {
        UserDto user = adminService.suspendUser(id, reasonOf(request));
        return ResponseEntity.ok(ApiResponse.success("User suspended", user));
    }

    @PostMapping("/users/{id}/reactivate")
    @Operation(summary = "Reactivate user", description = "Set user status back to ACTIVE")
    public ResponseEntity<ApiResponse<UserDto>> reactivateUser(@PathVariable Long id) {
        UserDto user = adminService.reactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User reactivated", user));
    }

    // ─── Booking management ──────────────────────────────────────────────

    @GetMapping("/bookings")
    @Operation(summary = "List bookings", description = "List/search all bookings, paginated, filterable by status")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<BookingDto> bookings = adminService.getBookings(status, search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/bookings/{id}")
    @Operation(summary = "Booking detail", description = "Full booking detail")
    public ResponseEntity<ApiResponse<BookingDto>> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getBookingDetail(id)));
    }

    @PostMapping("/bookings/{id}/cancel")
    @Operation(summary = "Force-cancel booking",
            description = "Admin force-cancel with optional reason; reuses the normal cancellation side-effects without the customer/technician permission rules")
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminReasonRequest request) {
        BookingDto booking = adminService.cancelBooking(id, reasonOf(request));
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", booking));
    }

    @PostMapping("/bookings/{id}/status")
    @Operation(summary = "Force booking status",
            description = "Admin escape hatch: force a booking to a given status, bypassing role and state-transition restrictions while keeping normal side-effects")
    public ResponseEntity<ApiResponse<BookingDto>> forceBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingUpdateRequest request) {
        BookingDto booking = adminService.forceBookingStatus(id, request.getStatus(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Booking status updated", booking));
    }

    // ─── Account deletion requests ───────────────────────────────────────

    @GetMapping("/deletion-requests")
    @Operation(summary = "List deletion requests",
            description = "Account-deletion requests submitted via the public page, optionally filtered by status (oldest-first when filtered, e.g. status=PENDING)")
    public ResponseEntity<ApiResponse<List<DeletionRequestDto>>> getDeletionRequests(
            @RequestParam(required = false) DeletionRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<DeletionRequestDto> requests = adminService.getDeletionRequests(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @PostMapping("/deletion-requests/{id}/process")
    @Operation(summary = "Process deletion request",
            description = "Anonymize/delete the account matching this request and mark it PROCESSED")
    public ResponseEntity<ApiResponse<DeletionRequestDto>> processDeletionRequest(@PathVariable Long id) {
        DeletionRequestDto result = adminService.processDeletionRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Deletion request processed", result));
    }

    @PostMapping("/deletion-requests/{id}/reject")
    @Operation(summary = "Reject deletion request",
            description = "Mark a request REJECTED without deleting any account (e.g. spam or no matching account)")
    public ResponseEntity<ApiResponse<DeletionRequestDto>> rejectDeletionRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminReasonRequest request) {
        DeletionRequestDto result = adminService.rejectDeletionRequest(id, reasonOf(request));
        return ResponseEntity.ok(ApiResponse.success("Deletion request rejected", result));
    }

    // ─── Audit log ───────────────────────────────────────────────────────

    @GetMapping("/logs")
    @Operation(summary = "Admin action logs", description = "Paginated audit trail of state-changing admin actions (newest first)")
    public ResponseEntity<ApiResponse<List<AdminActionLogDto>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(adminService.getActionLogs(pageable)));
    }

    private String reasonOf(AdminReasonRequest request) {
        return request != null ? request.getReason() : null;
    }
}
