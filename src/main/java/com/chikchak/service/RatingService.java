package com.chikchak.service;

import com.chikchak.dto.RatingDto;
import com.chikchak.dto.SubmitRatingRequest;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.RatingMapper;
import com.chikchak.model.*;
import com.chikchak.repository.BookingRepository;
import com.chikchak.repository.RatingRepository;
import com.chikchak.repository.TechnicianProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final AuthService authService;
    private final RatingMapper ratingMapper;

    public RatingDto submitRating(SubmitRatingRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can submit ratings");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        // Verify ownership
        if (!booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ChikChakException("Access denied to this booking");
        }

        // Verify booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ChikChakException("Can only rate completed bookings");
        }

        // Verify not already rated
        if (Boolean.TRUE.equals(booking.getIsRated())) {
            throw new ChikChakException("This booking has already been rated");
        }

        // Build and save rating
        Rating rating = Rating.builder()
                .booking(booking)
                .customer(currentUser)
                .technician(booking.getTechnician())
                .score(request.getScore())
                .comment(request.getComment())
                .build();

        try {
            rating = ratingRepository.save(rating);
        } catch (DataIntegrityViolationException e) {
            throw new ChikChakException("This booking has already been rated");
        }

        // Mark booking as rated
        booking.setIsRated(true);
        bookingRepository.save(booking);

        // Recalculate technician's average rating
        recalculateTechnicianRating(booking.getTechnician());

        log.info("Rating submitted for booking {} by customer {}: score={}",
                booking.getBookingNumber(), currentUser.getEmail(), request.getScore());

        return ratingMapper.toDto(rating);
    }

    private void recalculateTechnicianRating(TechnicianProfile technician) {
        BigDecimal avg = ratingRepository.calculateAverageRatingByTechnicianId(technician.getId());
        Long count = ratingRepository.countByTechnicianId(technician.getId());
        technician.setTotalRatings(count != null ? count.intValue() : 0);
        if (avg != null) {
            technician.setAverageRating(avg.setScale(2, RoundingMode.HALF_UP));
        } else {
            technician.setAverageRating(null);
        }
        technicianProfileRepository.save(technician);
        log.info("Recalculated average rating for technician {}: {} ({} ratings)", technician.getId(), technician.getAverageRating(), technician.getTotalRatings());
    }

    @Transactional(readOnly = true)
    public List<RatingDto> getMyRatings() {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can view their ratings");
        }

        return ratingRepository.findByCustomerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(ratingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RatingDto> getTechnicianRatings(Long technicianId) {
        technicianProfileRepository.findById(technicianId)
                .orElseThrow(() -> new ChikChakException("Technician not found"));

        return ratingRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId)
                .stream()
                .map(ratingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RatingDto getRatingByBookingId(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        // Verify access: customer, technician, or admin
        boolean hasAccess = currentUser.getRole() == UserRole.ADMIN
                || booking.getCustomer().getId().equals(currentUser.getId())
                || (booking.getTechnician() != null
                    && booking.getTechnician().getUser().getId().equals(currentUser.getId()));

        if (!hasAccess) {
            throw new ChikChakException("Access denied to this booking");
        }

        Rating rating = ratingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ChikChakException("No rating found for this booking"));

        return ratingMapper.toDto(rating);
    }
}
