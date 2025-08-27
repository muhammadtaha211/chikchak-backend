package com.chikchak.controller;

import com.chikchak.dto.*;
import com.chikchak.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management APIs")
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new service booking")
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        BookingDto booking = bookingService.createBooking(request);
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully", booking));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Get booking details by ID")
    public ResponseEntity<ApiResponse<BookingDto>> getBooking(@PathVariable Long id) {
        BookingDto booking = bookingService.getBooking(id);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/number/{bookingNumber}")
    @Operation(summary = "Get booking by number", description = "Get booking details by booking number")
    public ResponseEntity<ApiResponse<BookingDto>> getBookingByNumber(@PathVariable String bookingNumber) {
        BookingDto booking = bookingService.getBookingByNumber(bookingNumber);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active bookings", description = "Get all active bookings for current user")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getActiveBookings() {
        List<BookingDto> bookings = bookingService.getActiveBookings();
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PostMapping("/respond")
    @Operation(summary = "Technician response", description = "Technician responds to booking request")
    public ResponseEntity<ApiResponse<BookingDto>> respondToBooking(@Valid @RequestBody TechnicianResponseRequest request) {
        BookingDto booking = bookingService.respondToBooking(request);
        return ResponseEntity.ok(ApiResponse.success("Response recorded successfully", booking));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update booking status", description = "Update booking status and details")
    public ResponseEntity<ApiResponse<BookingDto>> updateBookingStatus(@PathVariable Long id,
                                                                       @Valid @RequestBody BookingUpdateRequest request) {
        BookingDto booking = bookingService.updateBookingStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated", booking));
    }
}
