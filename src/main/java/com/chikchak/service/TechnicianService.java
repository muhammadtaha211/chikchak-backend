package com.chikchak.service;

import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.UserMapper;
import com.chikchak.model.*;
import com.chikchak.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TechnicianService {

    private final TechnicianProfileRepository technicianProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final BookingRepository bookingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuthService authService;
    private final UserMapper userMapper;
    private final PricingService pricingService;

    public TechnicianSearchResponse searchTechnicians(TechnicianSearchRequest request) {
        List<TechnicianProfile> technicians;

        if (request.getServiceCategoryId() != null && request.getLatitude() != null && request.getLongitude() != null) {
            technicians = technicianProfileRepository.findBestTechniciansForServiceAndLocation(
                    request.getServiceCategoryId(),
                    request.getLatitude(),
                    request.getLongitude()
            );
        } else if (request.getServiceCategoryId() != null) {
            technicians = technicianProfileRepository.findAvailableTechniciansByService(request.getServiceCategoryId());
        } else {
            technicians = technicianProfileRepository.findByIsVerifiedAndIsAvailable(true, true);
        }

        // Apply filters
        List<TechnicianProfileDto> technicianDtos = technicians.stream()
                .filter(t -> applyFilters(t, request))
                .map(t -> {
                    TechnicianProfileDto dto = userMapper.toDto(t);
                    if (request.getLatitude() != null && request.getLongitude() != null) {
                        dto.setDistanceFromCustomer(calculateDistance(
                                request.getLatitude(), request.getLongitude(),
                                t.getUser().getLatitude(), t.getUser().getLongitude()
                        ));
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        // Apply sorting
        technicianDtos = applySorting(technicianDtos, request);

        return TechnicianSearchResponse.builder()
                .technicians(technicianDtos)
                .totalElements(technicianDtos.size())
                .totalPages((int) Math.ceil((double) technicianDtos.size() / request.getSize()))
                .currentPage(request.getPage())
                .build();
    }

    private boolean applyFilters(TechnicianProfile technician, TechnicianSearchRequest request) {
        if (request.getMaxPrice() != null &&
                technician.getBaseDiagnosisFee().compareTo(request.getMaxPrice()) > 0) {
            return false;
        }

        if (request.getMinRating() != null && technician.getAverageRating() != null &&
                technician.getAverageRating().doubleValue() < request.getMinRating()) {
            return false;
        }

        // Add more filters as needed

        return true;
    }

    private List<TechnicianProfileDto> applySorting(List<TechnicianProfileDto> technicians, TechnicianSearchRequest request) {
        switch (request.getSortBy().toLowerCase()) {
            case "distance":
                return technicians.stream()
                        .sorted((t1, t2) -> {
                            Double d1 = t1.getDistanceFromCustomer();
                            Double d2 = t2.getDistanceFromCustomer();
                            if (d1 == null) d1 = Double.MAX_VALUE;
                            if (d2 == null) d2 = Double.MAX_VALUE;
                            return d1.compareTo(d2);
                        })
                        .collect(Collectors.toList());
            case "price":
                return technicians.stream()
                        .sorted((t1, t2) -> t1.getBaseDiagnosisFee().compareTo(t2.getBaseDiagnosisFee()))
                        .collect(Collectors.toList());
            case "rating":
            default:
                return technicians.stream()
                        .sorted((t1, t2) -> {
                            if (t2.getAverageRating() == null && t1.getAverageRating() == null) return 0;
                            if (t2.getAverageRating() == null) return -1;
                            if (t1.getAverageRating() == null) return 1;
                            return t2.getAverageRating().compareTo(t1.getAverageRating());
                        })
                        .collect(Collectors.toList());
        }
    }

    private Double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }

        return pricingService.calculateDistance(lat1, lng1, lat2, lng2).doubleValue();
    }

    public TechnicianProfileDto updateTechnicianProfile(UpdateTechnicianProfileRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.TECHNICIAN) {
            throw new ChikChakException("Only technicians can update technician profile");
        }

        TechnicianProfile profile = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        // Update fields
        if (request.getBaseDiagnosisFee() != null) {
            profile.setBaseDiagnosisFee(request.getBaseDiagnosisFee());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getYearsExperience() != null) {
            profile.setYearsExperience(request.getYearsExperience());
        }
        if (request.getIsAvailable() != null) {
            profile.setIsAvailable(request.getIsAvailable());
        }
        if (request.getServiceRadius() != null) {
            profile.setServiceRadius(request.getServiceRadius());
        }
        if (request.getWorkingHours() != null) {
            profile.setWorkingHours(request.getWorkingHours());
        }

        // Update services
        if (request.getServiceIds() != null) {
            Set<ServiceCategory> services = new HashSet<>();
            for (Long serviceId : request.getServiceIds()) {
                ServiceCategory service = serviceCategoryRepository.findById(serviceId)
                        .orElseThrow(() -> new ChikChakException("Service category not found: " + serviceId));
                services.add(service);
            }
            profile.setServices(services);
        }

        profile = technicianProfileRepository.save(profile);
        log.info("Technician profile updated for user: {}", currentUser.getEmail());

        return userMapper.toDto(profile);
    }

    public void updateAvailability(Boolean available) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.TECHNICIAN) {
            throw new ChikChakException("Only technicians can update availability");
        }

        TechnicianProfile profile = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        profile.setIsAvailable(available);
        technicianProfileRepository.save(profile);

        log.info("Technician {} availability set to: {}", currentUser.getEmail(), available);
    }

    public List<BookingDto> getTechnicianBookings(Pageable pageable) {
        User currentUser = authService.getCurrentUserEntity();

        TechnicianProfile profile = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        return bookingRepository.findByTechnicianIdOrderByCreatedAtDesc(profile.getId(), pageable)
                .getContent().stream()
                .map(booking -> {
                    // Use your booking mapper here
                    return new BookingDto(); // Placeholder
                })
                .collect(Collectors.toList());
    }

    public TechnicianDashboardDto getTechnicianDashboard() {
        User currentUser = authService.getCurrentUserEntity();

        TechnicianProfile profile = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        List<Booking> pendingRequests = bookingRepository.findPendingBookingsForTechnician(profile.getId());
        List<Booking> activeJobs = bookingRepository.findActiveBookingsForTechnician(profile.getId());

        // Get earnings data
        java.math.BigDecimal totalEarnings = paymentTransactionRepository
                .getTotalEarningsForTechnician(currentUser.getId())
                .orElse(java.math.BigDecimal.ZERO);

        return TechnicianDashboardDto.builder()
                .pendingRequests(pendingRequests.stream().map(b -> new BookingDto()).collect(Collectors.toList()))
                .activeJobs(activeJobs.stream().map(b -> new BookingDto()).collect(Collectors.toList()))
                .totalEarnings(totalEarnings)
                .totalJobs(profile.getTotalJobs())
                .averageRating(profile.getAverageRating())
                .newRequests(pendingRequests.size())
                .build();
    }
}