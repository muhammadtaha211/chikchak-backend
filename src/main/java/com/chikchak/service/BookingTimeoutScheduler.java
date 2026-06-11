package com.chikchak.service;

import com.chikchak.model.Booking;
import com.chikchak.model.BookingStatus;
import com.chikchak.model.PaymentStatus;
import com.chikchak.model.RefundStatus;
import com.chikchak.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingTimeoutScheduler {

    private final BookingRepository bookingRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000) // Check every 5 minutes
    @Transactional
    public void cancelTimedOutBookings() {
        cancelUnansweredBookingRequests();
        cancelUnansweredTimeProposals();
    }

    /**
     * Scenario 1: Technician didn't respond to a booking request within 2 hours.
     */
    private void cancelUnansweredBookingRequests() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);

        List<Booking> timedOutBookings = bookingRepository.findByStatusInAndCreatedAtBefore(
                List.of(BookingStatus.PENDING, BookingStatus.TECHNICIAN_NOTIFIED),
                twoHoursAgo
        );

        for (Booking booking : timedOutBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledDatetime(LocalDateTime.now());
            booking.setPaymentStatus(PaymentStatus.CANCELLED);
            initiateRefundProcess(booking);
            bookingRepository.save(booking);

            chatService.sendBookingUpdateMessage(
                    booking.getId(),
                    "AUTO_CANCELLED_NO_RESPONSE",
                    "ההזמנה בוטלה אוטומטית, בעל המקצוע לא הגיב תוך שעתיים."
            );

            notificationService.notifyBookingCancellation(booking);

            log.info("Auto-cancelled booking {} — technician did not respond within 2 hours",
                    booking.getBookingNumber());
        }
    }

    /**
     * Scenario 2: Customer didn't respond to a technician's time proposal within 24 hours.
     */
    private void cancelUnansweredTimeProposals() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        List<Booking> timedOutProposals = bookingRepository.findByStatusAndProposedDatetimeBefore(
                BookingStatus.TIME_DISCUSSION,
                twentyFourHoursAgo
        );

        for (Booking booking : timedOutProposals) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledDatetime(LocalDateTime.now());
            booking.setPaymentStatus(PaymentStatus.CANCELLED);
            initiateRefundProcess(booking);
            bookingRepository.save(booking);

            chatService.sendBookingUpdateMessage(
                    booking.getId(),
                    "AUTO_CANCELLED_PROPOSAL_EXPIRED",
                    "ההזמנה בוטלה אוטומטית, הלקוח לא הגיב להצעת המועד תוך 24 שעות."
            );

            notificationService.notifyBookingCancellation(booking);

            log.info("Auto-cancelled booking {} — customer did not respond to time proposal within 24 hours",
                    booking.getBookingNumber());
        }
    }

    private void initiateRefundProcess(Booking booking) {
        // Guard: zero-fee bookings have nothing to refund
        if (booking.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
            log.info("Skipping refund for booking {} — totalAmount is zero", booking.getBookingNumber());
            return;
        }

        // Guard: only initiate if no refund is already in flight or completed
        if (booking.getRefundStatus() != RefundStatus.NOT_APPLICABLE) {
            log.warn("Refund already initiated for booking {} (status: {}), skipping",
                    booking.getBookingNumber(), booking.getRefundStatus());
            return;
        }

        booking.setRefundStatus(RefundStatus.PENDING_CHOICE);
        log.info("Refund initiated for booking {} — awaiting customer choice", booking.getBookingNumber());
    }
}
