package com.chikchak.service;

import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.BookingMapper;
import com.chikchak.model.*;
import com.chikchak.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final UserRepository userRepository;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final AuthService authService;
    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;
    private final PricingService pricingService;
    private final ChatService chatService;

    @Value("${chikchak.service-fee:20}")
    private BigDecimal serviceFee;

    // ⭐ NEW METHOD: Calculate Booking Price (before creating booking)
    public PricingCalculationDto calculateBookingPrice(PriceCalculationRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        // Get technician profile
        TechnicianProfile technician = technicianProfileRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new ChikChakException("Technician not found"));

        // Calculate base pricing
        PricingService.PricingCalculation pricing = pricingService.calculateBookingPrice(
                request.getCustomerLatitude(),
                request.getCustomerLongitude(),
                technician.getUser().getLatitude(),
                technician.getUser().getLongitude(),
                technician.getBaseDiagnosisFee()
        );

        // Build response DTO
        PricingCalculationDto result = PricingCalculationDto.builder()
                .diagnosisFee(pricing.getDiagnosisFee())
                .distanceKm(pricing.getDistanceKm())
                .distanceFee(pricing.getDistanceFee())
                .serviceFee(pricing.getServiceFee())
                .totalAmount(pricing.getTotalAmount())
                .creditsApplied(BigDecimal.ZERO)
                .finalTotal(pricing.getTotalAmount())
                .build();

        // Apply credits if requested
        if (request.getUseCredits() && currentUser.getAccountCredits().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal creditsToUse = currentUser.getAccountCredits().min(pricing.getTotalAmount());
            result.setCreditsApplied(creditsToUse);
            result.setFinalTotal(pricing.getTotalAmount().subtract(creditsToUse));
        }

        log.info("Price calculated for technician {} and customer {}: Total={}, Credits={}, Final={}",
                technician.getId(), currentUser.getId(),
                result.getTotalAmount(), result.getCreditsApplied(), result.getFinalTotal());

        return result;
    }

    // ⭐ UPDATED METHOD: Create Booking (with technician pre-selected)
    public BookingDto createBooking(CreateBookingRequest request) {
        User customer = authService.getCurrentUserEntity();

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can create bookings");
        }

        // Enforce single active booking per customer
        List<Booking> existingBookings = bookingRepository.findNonTerminalBookingsForCustomer(customer.getId());
        if (!existingBookings.isEmpty()) {
            throw new ChikChakException("You already have an active booking. Please complete or cancel it before creating a new one.");
        }

        ServiceCategory serviceCategory = serviceCategoryRepository.findByServiceType(request.getServiceType())
                .orElseThrow(() -> new ChikChakException("Service category not found for type: " + request.getServiceType()));

        // Get the selected technician
        TechnicianProfile selectedTechnician = technicianProfileRepository.findById(request.getSelectedTechnicianId())
                .orElseThrow(() -> new ChikChakException("Selected technician not found"));

        // Verify technician is available and verified
        if (!selectedTechnician.getIsVerified()) {
            throw new ChikChakException("Selected technician is not verified");
        }
        if (!selectedTechnician.getIsAvailable()) {
            throw new ChikChakException("Selected technician is not currently available");
        }

        // Calculate pricing
        PricingService.PricingCalculation pricing = pricingService.calculateBookingPrice(
                request.getServiceLatitude(),
                request.getServiceLongitude(),
                selectedTechnician.getUser().getLatitude(),
                selectedTechnician.getUser().getLongitude(),
                selectedTechnician.getBaseDiagnosisFee()
        );

        // Create booking number
        String bookingNumber = generateBookingNumber();



        // Create booking
        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .customer(customer)
                .technician(selectedTechnician)
                .serviceCategory(serviceCategory)
                .problemDescription(request.getProblemDescription())
                .status(BookingStatus.PENDING) // Awaiting technician confirmation
                .urgency(request.getUrgency())
                .serviceAddress(request.getServiceAddress())
                .serviceLatitude(request.getServiceLatitude())
                .serviceLongitude(request.getServiceLongitude())
                .preferredDatetime(request.getPreferredDatetime())
                .scheduledDatetime(request.getPreferredDatetime()) // Defaults to preferred, updated if customer accepts a proposed time
                .isEmergency(request.getIsEmergency())
                // Set pricing
                .diagnosisFee(pricing.getDiagnosisFee())
                .serviceFee(pricing.getServiceFee())
                .distanceKm(pricing.getDistanceKm())
                .distanceFee(pricing.getDistanceFee())
                .totalAmount(pricing.getTotalAmount())
                // Set payment info
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING) // Held until technician accepts
                .build();

        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            booking.setPhotos(String.join(",", request.getPhotos()));
        }

        booking = bookingRepository.save(booking);

        // Create status history
        createStatusHistory(booking, null, BookingStatus.PENDING,
                "Booking created - awaiting technician confirmation");

        // Apply credits if requested
        if (request.getUseAccountCredits() && customer.getAccountCredits().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal creditsToUse = customer.getAccountCredits().min(pricing.getTotalAmount());

            // Deduct from customer account
            customer.setAccountCredits(customer.getAccountCredits().subtract(creditsToUse));
            userRepository.save(customer);

            // Track credits applied (keep totalAmount as original price)
            booking.setCreditsApplied(creditsToUse);
            booking.setUseAccountCredits(true);
            booking = bookingRepository.save(booking);

            log.info("Applied {} credits to booking {}", creditsToUse, bookingNumber);
        }

        // Create chat immediately
        Chat chat = chatService.findOrCreateChatForBooking(booking.getId());

        // Send initial booking notification in chat
        chatService.sendBookingUpdateMessage(
                booking.getId(),
                "BOOKING_CREATED",
                String.format("Booking request for %s on %s",
                        serviceCategory.getName(),
                        formatTimeRange(booking.getPreferredDatetime())
                )
        );

        // Notify technician
        notificationService.notifyTechnicianOfNewBooking(booking);

        log.info("Booking created (PENDING): {} for technician: {} by customer: {}",
                bookingNumber, selectedTechnician.getUser().getEmail(), customer.getEmail());

        return bookingMapper.toDto(booking);
    }

    // UPDATED METHOD: Technician responds to booking
    public BookingDto respondToBooking(TechnicianResponseRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.TECHNICIAN) {
            throw new ChikChakException("Only technicians can respond to bookings");
        }

        TechnicianProfile technician = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        Booking booking = findBookingById(request.getBookingId());

        // Verify this booking is for this technician
        if (!booking.getTechnician().getId().equals(technician.getId())) {
            throw new ChikChakException("This booking is not assigned to you");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ChikChakException("This booking is no longer pending");
        }

        switch (request.getResponseType()) {
            case ACCEPT:
                return acceptBooking(booking, request.getSuggestedTime());
            case DISCUSS_TIME:
                return discussBookingTime(booking, request.getSuggestedTime(), request.getMessage());
            case DECLINE:
                return declineBooking(booking, request.getMessage());
            default:
                throw new ChikChakException("Invalid response type");
        }
    }

    private BookingDto acceptBooking(Booking booking, LocalDateTime suggestedTime) {
        // Use suggested time if provided, otherwise use preferred time
        if (suggestedTime != null) {
            booking.setScheduledDatetime(suggestedTime);
        } else {
            booking.setScheduledDatetime(booking.getPreferredDatetime());
        }

        booking.setStatus(BookingStatus.SCHEDULED);
        booking = bookingRepository.save(booking);

        createStatusHistory(booking, BookingStatus.PENDING, BookingStatus.SCHEDULED,
                "Technician accepted booking");

        // Send chat message
        chatService.sendBookingUpdateMessage(
                booking.getId(),
                "BOOKING_ACCEPTED",
                "Professional accepted! Scheduled for " + formatTimeRange(booking.getScheduledDatetime())
        );

        // Notify customer
        notificationService.notifyCustomerBookingAccepted(booking);

        log.info("Booking {} accepted by technician", booking.getBookingNumber());

        return bookingMapper.toDto(booking);
    }

    private BookingDto discussBookingTime(Booking booking, LocalDateTime suggestedTime, String message) {
        booking.setStatus(BookingStatus.TIME_DISCUSSION);
        booking.setProposedDatetime(suggestedTime);
        booking = bookingRepository.save(booking);

        createStatusHistory(booking, BookingStatus.PENDING, BookingStatus.TIME_DISCUSSION,
                "Technician requested time discussion");

        // Send time proposal in chat
        chatService.sendTimeProposalMessage(
                booking.getId(),
                suggestedTime,
                message
        );

        // Notify customer
        notificationService.notifyCustomerTimeProposal(booking);

        log.info("Technician requested time discussion for booking {}", booking.getBookingNumber());

        return bookingMapper.toDto(booking);
    }

    private BookingDto declineBooking(Booking booking, String reason) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        createStatusHistory(booking, BookingStatus.PENDING, BookingStatus.CANCELLED,
                "Technician declined: " + (reason != null ? reason : "No reason provided"));

        // Initiate refund process
        initiateRefundProcess(booking);

        // Send chat message
        chatService.sendBookingUpdateMessage(
                booking.getId(),
                "BOOKING_DECLINED",
                "Technician declined this booking. " +
                        (reason != null ? "Reason: " + reason : "")
        );

        // Notify customer to choose refund method
        notificationService.notifyCustomerBookingDeclined(booking);

        log.info("Booking {} declined by technician: {}", booking.getBookingNumber(), reason);

        return bookingMapper.toDto(booking);
    }

    //  Initiate refund for declined booking
    private void initiateRefundProcess(Booking booking) {
        // Guard: only initiate if no refund is already in flight or completed
        if (booking.getRefundStatus() != RefundStatus.NOT_APPLICABLE) {
            log.warn("Refund already initiated for booking {} (status: {}), skipping",
                    booking.getBookingNumber(), booking.getRefundStatus());
            return;
        }

        // Mark as pending customer's refund choice — do NOT process refund yet
        booking.setRefundStatus(RefundStatus.PENDING_CHOICE);
        bookingRepository.save(booking);

        log.info("Refund initiated for booking {} — awaiting customer choice", booking.getBookingNumber());
    }

    // Process refund with customer's choice
    public void processRefund(Long bookingId, RefundChoiceRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        Booking booking = findBookingById(bookingId);

        // Verify it's the customer's booking
        if (!booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ChikChakException("Access denied");
        }

        // Guard: only process if refund is pending customer choice
        if (booking.getRefundStatus() != RefundStatus.PENDING_CHOICE) {
            throw new ChikChakException("No pending refund choice for this booking");
        }

        BigDecimal refundAmount = booking.getTotalAmount();
        BigDecimal creditsApplied = booking.getCreditsApplied() != null ? booking.getCreditsApplied() : BigDecimal.ZERO;

        if (request.getRefundMethod() == RefundMethod.CREDITS) {
            // Refund full totalAmount as credits
            userRepository.addAccountCredits(currentUser.getId(), refundAmount);

            booking.setRefundStatus(RefundStatus.PROCESSED_CREDITS);
            log.info("Refunded {} to credits for booking {}", refundAmount, bookingId);
        } else {
            // BANK refund — split if credits were used
            if (creditsApplied.compareTo(BigDecimal.ZERO) > 0) {
                // Return credits portion to account credits
                userRepository.addAccountCredits(currentUser.getId(), creditsApplied);
                log.info("Returned {} credits to account for booking {}", creditsApplied, bookingId);

                // Process only the card portion as bank refund
                BigDecimal cardRefundAmount = refundAmount.subtract(creditsApplied);
                if (cardRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // TODO: Integrate with payment processor API
                    log.info("Bank refund initiated for {} for booking {}", cardRefundAmount, bookingId);
                }
            } else {
                // No credits used — full bank refund
                // TODO: Integrate with payment processor API
                log.info("Bank refund initiated for {} for booking {}", refundAmount, bookingId);
            }

            booking.setRefundStatus(RefundStatus.PROCESSED_CARD);
        }

        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);

        // Send confirmation
        String refundDescription;
        if (request.getRefundMethod() == RefundMethod.CREDITS) {
            refundDescription = String.format("Refund of ₪%.2f processed via account credits", refundAmount);
        } else if (creditsApplied.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cardPortion = refundAmount.subtract(creditsApplied);
            refundDescription = String.format("Refund processed: ₪%.2f to account credits + ₪%.2f to original payment method",
                    creditsApplied, cardPortion);
        } else {
            refundDescription = String.format("Refund of ₪%.2f processed via original payment method", refundAmount);
        }

        chatService.sendBookingUpdateMessage(
                bookingId,
                "REFUND_PROCESSED",
                refundDescription
        );
    }

    // Keep all your existing methods below...
    // (getBooking, getBookingByNumber, updateBookingStatus, proposeTime, etc.)

    public BookingDto getBooking(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();
        Booking booking = findBookingById(bookingId);

        validateBookingAccess(booking, currentUser);

        BookingDto bookingDto = bookingMapper.toDto(booking);
        bookingDto.setUnreadMessagesCount(0); // Placeholder

        return bookingDto;
    }

    public BookingDto getBookingByNumber(String bookingNumber) {
        User currentUser = authService.getCurrentUserEntity();
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        validateBookingAccess(booking, currentUser);

        return bookingMapper.toDto(booking);
    }

    public List<BookingDto> getCustomerBookings(Long customerId, Pageable pageable) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() == UserRole.CUSTOMER && !currentUser.getId().equals(customerId)) {
            throw new ChikChakException("Access denied");
        }

        Page<Booking> bookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        return bookings.getContent().stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getTechnicianBookings(Long technicianId, Pageable pageable) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() == UserRole.TECHNICIAN) {
            TechnicianProfile profile = technicianProfileRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ChikChakException("Technician profile not found"));
            if (!profile.getId().equals(technicianId)) {
                throw new ChikChakException("Access denied");
            }
        }

        Page<Booking> bookings = bookingRepository.findByTechnicianIdOrderByCreatedAtDesc(technicianId, pageable);
        return bookings.getContent().stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public BookingDto updateBookingStatus(Long bookingId, BookingUpdateRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        Booking booking = findBookingById(bookingId);

        validateBookingAccess(booking, currentUser);

        BookingStatus oldStatus = booking.getStatus();
        BookingStatus newStatus = request.getStatus();

        validateStatusTransition(oldStatus, newStatus, currentUser.getRole());

        booking.setStatus(newStatus);

        if (request.getScheduledDatetime() != null) {
            booking.setScheduledDatetime(request.getScheduledDatetime());
        }

        if (request.getTechnicianNotes() != null) {
            booking.setTechnicianNotes(request.getTechnicianNotes());
        }

        handleSpecialStatusUpdates(booking, newStatus);

        booking = bookingRepository.save(booking);

        createStatusHistory(booking, oldStatus, newStatus, request.getReason());

        notificationService.notifyBookingStatusChanged(booking, oldStatus, newStatus);

        log.info("Booking {} status changed from {} to {} by {}",
                booking.getBookingNumber(), oldStatus, newStatus, currentUser.getEmail());

        return bookingMapper.toDto(booking);
    }

    private void handleSpecialStatusUpdates(Booking booking, BookingStatus newStatus) {
        switch (newStatus) {
            case ARRIVED:
                booking.setPaymentStatus(PaymentStatus.ESCROW);
                booking.setArrivalMarkedAt(LocalDateTime.now());
                chatService.sendBookingUpdateMessage(
                        booking.getId(),
                        "ARRIVAL_CONFIRMATION",
                        "The professional has marked that they arrived. Please confirm their arrival."
                );
                break;
            case COMPLETED:
                booking.setCompletedDatetime(LocalDateTime.now());
                updateTechnicianStats(booking.getTechnician());
                break;
            case CANCELLED:
                // If payment was pending or in escrow, initiate refund for the customer
                if (booking.getPaymentStatus() == PaymentStatus.PENDING ||
                        booking.getPaymentStatus() == PaymentStatus.ESCROW) {
                    initiateRefundProcess(booking);
                }
                notificationService.notifyBookingCancellation(booking);
                break;

        }
    }

    private void updateTechnicianStats(TechnicianProfile technician) {
        if (technician != null) {
            Long completedJobs = bookingRepository.countCompletedBookingsByTechnicianId(technician.getId());
            technician.setTotalJobs(completedJobs.intValue());
            technicianProfileRepository.save(technician);
        }
    }

    private void validateStatusTransition(BookingStatus oldStatus, BookingStatus newStatus, UserRole userRole) {
        // Can't change cancelled bookings
        if (oldStatus == BookingStatus.CANCELLED) {
            throw new ChikChakException("Cannot modify cancelled booking");
        }

        // Can't change completed bookings
        if (oldStatus == BookingStatus.COMPLETED) {
            throw new ChikChakException("Cannot modify completed booking");
        }

        if (userRole == UserRole.CUSTOMER) {
            if (newStatus != BookingStatus.CANCELLED) {
                throw new ChikChakException("Customers can only cancel bookings");
            }
        } else if (userRole == UserRole.TECHNICIAN) {
            if (newStatus == BookingStatus.PENDING) {
                throw new ChikChakException("Invalid status transition");
            }
        }
    }

    private void createStatusHistory(Booking booking, BookingStatus oldStatus, BookingStatus newStatus, String reason) {
        BookingStatusHistory history = BookingStatusHistory.builder()
                .booking(booking)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(authService.getCurrentUserEntity().getEmail())
                .reason(reason)
                .build();

        statusHistoryRepository.save(history);
    }

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));
    }

    private void validateBookingAccess(Booking booking, User user) {
        boolean hasAccess = false;

        switch (user.getRole()) {
            case ADMIN:
                hasAccess = true;
                break;
            case CUSTOMER:
                hasAccess = booking.getCustomer().getId().equals(user.getId());
                break;
            case TECHNICIAN:
                hasAccess = booking.getTechnician() != null &&
                        booking.getTechnician().getUser().getId().equals(user.getId());
                break;
        }

        if (!hasAccess) {
            throw new ChikChakException("Access denied to this booking");
        }
    }

    private String generateBookingNumber() {
        return "CHK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    public List<BookingDto> getActiveBookings() {
        User currentUser = authService.getCurrentUserEntity();
        List<Booking> bookings;

        switch (currentUser.getRole()) {
            case CUSTOMER:
                bookings = bookingRepository.findActiveBookingsForCustomer(currentUser.getId());
                break;
            case TECHNICIAN:
                TechnicianProfile profile = technicianProfileRepository.findByUserId(currentUser.getId())
                        .orElseThrow(() -> new ChikChakException("Technician profile not found"));
                bookings = bookingRepository.findActiveBookingsForTechnician(profile.getId());
                break;
            default:
                throw new ChikChakException("Invalid user role for this operation");
        }

        return bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getUnresolvedRefunds() {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can view unresolved refunds");
        }

        List<Booking> bookings = bookingRepository.findByCustomerIdAndRefundStatus(
                currentUser.getId(), RefundStatus.PENDING_CHOICE);

        return bookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getPendingBookings() {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.TECHNICIAN) {
            throw new ChikChakException("Only technicians can view pending bookings");
        }

        TechnicianProfile profile = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        List<Booking> pendingBookings = bookingRepository.findPendingBookingsForTechnician(profile.getId());

        return pendingBookings.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public CustomerDashboardDto getCustomerDashboard() {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can access customer dashboard");
        }

        List<Booking> activeBookings = bookingRepository.findActiveBookingsForCustomer(currentUser.getId());
        List<Booking> recentBookings = bookingRepository.findByCustomerId(currentUser.getId());

        Long totalBookings = (long) recentBookings.size();
        Long completedBookings = recentBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();

        return CustomerDashboardDto.builder()
                .activeBookings(activeBookings.stream().map(bookingMapper::toDto).collect(Collectors.toList()))
                .recentBookings(recentBookings.stream()
                        .limit(10)
                        .map(bookingMapper::toDto)
                        .collect(Collectors.toList()))
                .accountCredits(currentUser.getAccountCredits())
                .totalBookings(totalBookings.intValue())
                .completedBookings(completedBookings.intValue())
                .build();
    }

    public BookingDto proposeTime(Long bookingId, ProposeTimeRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.TECHNICIAN) {
            throw new ChikChakException("Only technicians can propose times");
        }

        Booking booking = findBookingById(bookingId);



        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.TIME_DISCUSSION) {
            throw new ChikChakException("Cannot propose time for booking with status: " + booking.getStatus());
        }


        TechnicianProfile technician = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        if (!booking.getTechnician().getId().equals(technician.getId())) {
            throw new ChikChakException("This booking is not assigned to you");
        }

        booking.setProposedDatetime(request.getProposedDatetime());
        booking.setStatus(BookingStatus.TIME_DISCUSSION);
        booking = bookingRepository.save(booking);

        chatService.sendTimeProposalMessage(
                bookingId,
                request.getProposedDatetime(),
                request.getMessage()
        );

        notificationService.notifyCustomerTimeProposal(booking);

        log.info("Time proposed for booking {}: {}", bookingId, request.getProposedDatetime());

        return bookingMapper.toDto(booking);
    }

    public BookingDto acceptProposedTime(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can accept proposals");
        }

        Booking booking = findBookingById(bookingId);



        if (booking.getStatus() != BookingStatus.TIME_DISCUSSION) {
            throw new ChikChakException("No active time discussion for this booking");
        }

        if (booking.getProposedDatetime() == null) {
            throw new ChikChakException("No time proposal to accept");
        }


        if (booking.getProposedDatetime() == null) {
            throw new ChikChakException("No time proposal to accept");
        }

        booking.setScheduledDatetime(booking.getProposedDatetime());
        booking.setProposedDatetime(null);
        booking.setStatus(BookingStatus.ACCEPTED);
        booking = bookingRepository.save(booking);

        chatService.sendBookingUpdateMessage(
                bookingId,
                "TIME_ACCEPTED",
                "Time confirmed: " + formatTimeRange(booking.getScheduledDatetime())
        );

        notificationService.notifyTechnicianTimeAccepted(booking);

        log.info("Time accepted for booking {}", bookingId);

        return bookingMapper.toDto(booking);
    }

    public void declineAndRefund(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = findBookingById(bookingId);
        validateBookingAccess(booking, currentUser);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setProposedDatetime(null);
        bookingRepository.save(booking);

        // Set refundStatus = PENDING_CHOICE — customer chooses later via the refund page
        initiateRefundProcess(booking);

        notificationService.notifyBookingCancellation(booking);

        log.info("Booking {} cancelled — refund pending customer choice", bookingId);
    }

    private String formatTimeRange(LocalDateTime start) {
        if (start == null) return "TBD";
        LocalDateTime end = start.plusHours(2);
        return String.format("%s, %s - %s",
                start.format(DateTimeFormatter.ofPattern("MMM dd")),
                start.format(DateTimeFormatter.ofPattern("HH:mm")),
                end.format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }

    public BookingDto confirmArrival(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can confirm arrival");
        }

        Booking booking = findBookingById(bookingId);
        validateBookingAccess(booking, currentUser);

        if (booking.getStatus() != BookingStatus.ARRIVED) {
            throw new ChikChakException("Booking is not in ARRIVED status");
        }

        // Release payment
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking = bookingRepository.save(booking);

        createStatusHistory(booking, BookingStatus.ARRIVED, BookingStatus.IN_PROGRESS,
                "Customer confirmed arrival - payment released");

        chatService.sendBookingUpdateMessage(
                bookingId,
                "ARRIVAL_CONFIRMED",
                "Arrival confirmed. Payment released. The professional will now begin work."
        );

        notificationService.notifyPaymentRelease(booking);

        log.info("Arrival confirmed for booking {}, payment released", booking.getBookingNumber());

        return bookingMapper.toDto(booking);
    }

    public BookingDto dismissRating(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can dismiss ratings");
        }

        Booking booking = findBookingById(bookingId);
        validateBookingAccess(booking, currentUser);

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ChikChakException("Can only dismiss rating for completed bookings");
        }

        booking.setRatingDismissed(true);
        booking = bookingRepository.save(booking);

        log.info("Rating dismissed for booking {}", booking.getBookingNumber());

        return bookingMapper.toDto(booking);
    }

    public BookingDto disputeArrival(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can dispute arrival");
        }

        Booking booking = findBookingById(bookingId);
        validateBookingAccess(booking, currentUser);

        if (booking.getStatus() != BookingStatus.ARRIVED) {
            throw new ChikChakException("Booking is not in ARRIVED status");
        }

        booking.setStatus(BookingStatus.DISPUTED);
        booking.setPaymentStatus(PaymentStatus.DISPUTED);
        booking = bookingRepository.save(booking);

        createStatusHistory(booking, BookingStatus.ARRIVED, BookingStatus.DISPUTED,
                "Customer disputed technician arrival");

        chatService.sendBookingUpdateMessage(
                bookingId,
                "ARRIVAL_DISPUTED",
                "Arrival has been disputed. An admin will review this case."
        );

        log.info("Arrival disputed for booking {}", booking.getBookingNumber());

        return bookingMapper.toDto(booking);
    }

}