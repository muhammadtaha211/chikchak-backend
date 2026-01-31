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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    public BookingDto createBooking(CreateBookingRequest request) {
        User customer = authService.getCurrentUserEntity();
        
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can create bookings");
        }

        ServiceCategory serviceCategory = serviceCategoryRepository.findById(request.getServiceCategoryId())
            .orElseThrow(() -> new ChikChakException("Service category not found"));

        // Find available technicians for this service and location
        List<TechnicianProfile> availableTechnicians = technicianProfileRepository
            .findBestTechniciansForServiceAndLocation(
                request.getServiceCategoryId(),
                request.getServiceLatitude(),
                request.getServiceLongitude()
            );

        if (availableTechnicians.isEmpty()) {
            throw new ChikChakException("No technicians available for this service in your area");
        }

        // Create booking
        String bookingNumber = generateBookingNumber();
        
        Booking booking = Booking.builder()
            .bookingNumber(bookingNumber)
            .customer(customer)
            .serviceCategory(serviceCategory)
            .problemDescription(request.getProblemDescription())
            .status(BookingStatus.PENDING)
            .urgency(request.getUrgency())
            .serviceAddress(request.getServiceAddress())
            .serviceCity(request.getServiceCity())
            .serviceLatitude(request.getServiceLatitude())
            .serviceLongitude(request.getServiceLongitude())
            .preferredDatetime(request.getPreferredDatetime())
            .isEmergency(request.getIsEmergency())
            .customerNotes(request.getCustomerNotes())
            .serviceFee(serviceFee)
            .paymentStatus(PaymentStatus.PENDING)
            .build();

        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            booking.setPhotos(String.join(",", request.getPhotos()));
        }

        booking = bookingRepository.save(booking);
        
        // Create status history
        createStatusHistory(booking, null, BookingStatus.PENDING, "Booking created");
        
        // Notify technicians
        notificationService.notifyTechniciansOfNewBooking(booking, availableTechnicians);
        
        log.info("New booking created: {} by customer: {}", bookingNumber, customer.getEmail());
        
        return bookingMapper.toDto(booking);
    }






    public BookingDto getBooking(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();
        Booking booking = findBookingById(bookingId);
        
        // Check permissions
        validateBookingAccess(booking, currentUser);
        
        BookingDto bookingDto = bookingMapper.toDto(booking);
        
        // Add unread messages count
        // This would be implemented with the chat service
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
        
        // Customers can only see their own bookings, admins can see all
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
        
        // Technicians can only see their own bookings, admins can see all
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

    public BookingDto respondToBooking(TechnicianResponseRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        
        if (currentUser.getRole() != UserRole.TECHNICIAN) {
            throw new ChikChakException("Only technicians can respond to bookings");
        }
        
        TechnicianProfile technician = technicianProfileRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new ChikChakException("Technician profile not found"));
        
        Booking booking = findBookingById(request.getBookingId());
        
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.TECHNICIAN_NOTIFIED) {
            throw new ChikChakException("This booking is no longer available");
        }

        switch (request.getResponseType()) {
            case ACCEPT:
                return acceptBooking(booking, technician, request.getSuggestedTime());
            case DISCUSS_TIME:
                return discussBookingTime(booking, technician, request.getSuggestedTime(), request.getMessage());
            case DECLINE:
                return declineBooking(booking, technician, request.getMessage());
            default:
                throw new ChikChakException("Invalid response type");
        }
    }

    private BookingDto acceptBooking(Booking booking, TechnicianProfile technician, LocalDateTime suggestedTime) {
        booking.setTechnician(technician);
        booking.setStatus(BookingStatus.ACCEPTED);
        
        if (suggestedTime != null) {
            booking.setScheduledDatetime(suggestedTime);
            booking.setStatus(BookingStatus.SCHEDULED);
        }

        // Calculate pricing
        PricingService.PricingCalculation pricing = pricingService.calculateBookingPrice(
            booking.getServiceLatitude(),
            booking.getServiceLongitude(),
            technician.getUser().getLatitude(),
            technician.getUser().getLongitude(),
            technician.getBaseDiagnosisFee()
        );

        booking.setDiagnosisFee(pricing.getDiagnosisFee());
        booking.setDistanceKm(pricing.getDistanceKm());
        booking.setDistanceFee(pricing.getDistanceFee());
        booking.setTotalAmount(pricing.getTotalAmount());

        booking = bookingRepository.save(booking);
        
        createStatusHistory(booking, BookingStatus.PENDING, BookingStatus.ACCEPTED, "Technician accepted booking");
        
        // Notify customer
        notificationService.notifyCustomerBookingAccepted(booking);
        
        log.info("Booking {} accepted by technician {}", booking.getBookingNumber(), technician.getUser().getEmail());
        
        return bookingMapper.toDto(booking);
    }

    private BookingDto discussBookingTime(Booking booking, TechnicianProfile technician, 
                                         LocalDateTime suggestedTime, String message) {
        // This would typically create a chat message and notify the customer
        // For now, we'll just update the booking status
        booking.setTechnician(technician);
        booking.setStatus(BookingStatus.TECHNICIAN_NOTIFIED);
        
        booking = bookingRepository.save(booking);
        
        // Send message through chat system
        notificationService.notifyCustomerTimeDiscussion(booking, suggestedTime, message);
        
        log.info("Technician {} requested time discussion for booking {}", 
                technician.getUser().getEmail(), booking.getBookingNumber());
        
        return bookingMapper.toDto(booking);
    }

    private BookingDto declineBooking(Booking booking, TechnicianProfile technician, String message) {
        // Log the decline but don't change booking status - other technicians can still accept
        log.info("Technician {} declined booking {}: {}", 
                technician.getUser().getEmail(), booking.getBookingNumber(), message);
        
        // Notify other available technicians
        notificationService.notifyOtherTechniciansAfterDecline(booking, technician);
        
        return bookingMapper.toDto(booking);
    }

    public BookingDto updateBookingStatus(Long bookingId, BookingUpdateRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        Booking booking = findBookingById(bookingId);
        
        validateBookingAccess(booking, currentUser);
        
        BookingStatus oldStatus = booking.getStatus();
        BookingStatus newStatus = request.getStatus();
        
        // Validate status transition
        validateStatusTransition(oldStatus, newStatus, currentUser.getRole());
        
        booking.setStatus(newStatus);
        
        if (request.getScheduledDatetime() != null) {
            booking.setScheduledDatetime(request.getScheduledDatetime());
        }
        
        if (request.getTechnicianNotes() != null) {
            booking.setTechnicianNotes(request.getTechnicianNotes());
        }
        
        // Handle special status updates
        handleSpecialStatusUpdates(booking, newStatus);
        
        booking = bookingRepository.save(booking);
        
        createStatusHistory(booking, oldStatus, newStatus, request.getReason());
        
        // Send notifications
        notificationService.notifyBookingStatusChanged(booking, oldStatus, newStatus);
        
        log.info("Booking {} status changed from {} to {} by {}", 
                booking.getBookingNumber(), oldStatus, newStatus, currentUser.getEmail());
        
        return bookingMapper.toDto(booking);
    }

    private void handleSpecialStatusUpdates(Booking booking, BookingStatus newStatus) {
        switch (newStatus) {
            case ARRIVED:
                // Release diagnosis payment when technician arrives
                notificationService.notifyPaymentRelease(booking);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                break;
            case COMPLETED:
                booking.setCompletedDatetime(LocalDateTime.now());
                // Update technician stats
                updateTechnicianStats(booking.getTechnician());
                break;
            case CANCELLED:
                // Process refund
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
        // Define valid transitions based on user role
        // This is a simplified version - you might want a more sophisticated state machine
        
        if (userRole == UserRole.CUSTOMER) {
            // Customers can only cancel bookings
            if (newStatus != BookingStatus.CANCELLED) {
                throw new ChikChakException("Customers can only cancel bookings");
            }
        } else if (userRole == UserRole.TECHNICIAN) {
            // Technicians can update to most statuses except PENDING and CANCELLED
            if (newStatus == BookingStatus.PENDING) {
                throw new ChikChakException("Invalid status transition");
            }
        }
        // Admins can change to any status
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

        // Calculate stats
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

        // Update booking
        booking.setProposedDatetime(request.getProposedDatetime());
        booking.setStatus(BookingStatus.TIME_DISCUSSION);
        booking = bookingRepository.save(booking);

        // Create TIME_PROPOSAL message in chat
        chatService.sendTimeProposalMessage(
                bookingId,
                request.getProposedDatetime(),
                request.getMessage()
        );

        // Notify customer
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

        if (booking.getProposedDatetime() == null) {
            throw new ChikChakException("No time proposal to accept");
        }

        // Accept the proposal
        booking.setScheduledDatetime(booking.getProposedDatetime());
        booking.setProposedDatetime(null); // Clear proposal
        booking.setStatus(BookingStatus.ACCEPTED);
        booking = bookingRepository.save(booking);

        // Send confirmation message
        chatService.sendBookingUpdateMessage(
                bookingId,
                "TIME_ACCEPTED",
                "Time confirmed: " + booking.getScheduledDatetime()
        );

        // Notify technician
        notificationService.notifyTechnicianTimeAccepted(booking);

        log.info("Time accepted for booking {}", bookingId);

        return bookingMapper.toDto(booking);
    }

    public void declineAndRefund(Long bookingId, RefundChoiceRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = findBookingById(bookingId);
        validateBookingAccess(booking, currentUser);

        // Cancel booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setProposedDatetime(null);
        bookingRepository.save(booking);

        // Process refund based on choice
        if (request.getRefundMethod() == RefundMethod.CREDITS) {
            // Add to account credits
            currentUser.setAccountCredits(
                    currentUser.getAccountCredits().add(booking.getTotalAmount())
            );
            userRepository.save(currentUser);
        } else {
            // Process bank refund (integrate with payment gateway)
            // paymentService.processRefund(booking);
        }

        // Notify technician
        notificationService.notifyBookingCancellation(booking);

        log.info("Booking {} cancelled with {} refund", bookingId, request.getRefundMethod());
    }
}