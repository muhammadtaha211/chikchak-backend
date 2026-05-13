package com.chikchak.service;

import com.chikchak.model.Booking;
import com.chikchak.model.BookingStatus;
import com.chikchak.model.PaymentStatus;
import com.chikchak.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoConfirmScheduler {

    private final BookingRepository bookingRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000) // Check every minute
    @Transactional
    public void autoConfirmArrivals() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

        List<Booking> unconfirmedArrivals = bookingRepository
                .findByStatusAndArrivalMarkedAtBefore(BookingStatus.ARRIVED, tenMinutesAgo);

        for (Booking booking : unconfirmedArrivals) {
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            booking.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);

            // Disabled while app is in no-payment mode: message references payment release that no longer happens.
            // chatService.sendBookingUpdateMessage(
            //         booking.getId(),
            //         "AUTO_CONFIRMED",
            //         "Arrival auto-confirmed after 10 minutes. Payment released."
            // );

            notificationService.notifyPaymentRelease(booking);

            log.info("Auto-confirmed arrival for booking {}", booking.getBookingNumber());
        }
    }
}