package com.chikchak.controller;

import com.chikchak.dto.ApiResponse;
import com.chikchak.dto.RatingDto;
import com.chikchak.dto.SubmitRatingRequest;
import com.chikchak.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Tag(name = "Ratings", description = "Rating management APIs")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @Operation(summary = "Submit rating", description = "Customer rates a technician after a completed booking")
    public ResponseEntity<ApiResponse<RatingDto>> submitRating(@Valid @RequestBody SubmitRatingRequest request) {
        RatingDto rating = ratingService.submitRating(request);
        return ResponseEntity.ok(ApiResponse.success("Rating submitted successfully", rating));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my ratings", description = "Get all ratings submitted by the current customer")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getMyRatings() {
        List<RatingDto> ratings = ratingService.getMyRatings();
        return ResponseEntity.ok(ApiResponse.success(ratings));
    }

    @GetMapping("/technician/{technicianId}")
    @Operation(summary = "Get technician ratings", description = "Get all ratings for a specific technician")
    public ResponseEntity<ApiResponse<List<RatingDto>>> getTechnicianRatings(@PathVariable Long technicianId) {
        List<RatingDto> ratings = ratingService.getTechnicianRatings(technicianId);
        return ResponseEntity.ok(ApiResponse.success(ratings));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking rating", description = "Get the rating for a specific booking")
    public ResponseEntity<ApiResponse<RatingDto>> getRatingByBookingId(@PathVariable Long bookingId) {
        RatingDto rating = ratingService.getRatingByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success(rating));
    }
}
