package com.chikchak.service;

import com.chikchak.model.Booking;
import com.chikchak.model.TechnicianProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    //  Notify single technician of new booking
    public void notifyTechnicianOfNewBooking(com.chikchak.model.Booking booking) {
        if (booking.getTechnician() == null) {
            log.warn("Cannot notify technician - no technician assigned to booking {}", booking.getBookingNumber());
            return;
        }

        com.chikchak.model.TechnicianProfile technician = booking.getTechnician();

        log.info("Notifying technician {} of new booking: {}",
                technician.getUser().getEmail(), booking.getBookingNumber());

        // In a real implementation, this would:
        // 1. Send push notification to technician mobile app
        // 2. Send SMS notification for urgent bookings
        // 3. Send email notification
        // 4. Update real-time dashboard via WebSocket

        String message = String.format(
                "New booking request #%s - %s at %s on %s",
                booking.getBookingNumber(),
                booking.getServiceCategory().getName(),
                booking.getServiceAddress(),
                booking.getPreferredDatetime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
        );

        sendTechnicianNotification(technician, booking, message);
    }

    //  Notify customer that technician declined
    public void notifyCustomerBookingDeclined(com.chikchak.model.Booking booking) {
        log.info("Notifying customer {} that booking {} was declined",
                booking.getCustomer().getEmail(), booking.getBookingNumber());

        String message = "Your technician declined the booking. Please choose a refund method.";

        sendCustomerNotification(booking.getCustomer(), message);
    }


    public void notifyCustomerBookingAccepted(com.chikchak.model.Booking booking) {
        log.info("Notifying customer {} that booking {} was accepted",
                booking.getCustomer().getEmail(), booking.getBookingNumber());

        sendCustomerNotification(booking.getCustomer(),
                "Your booking has been accepted by " + booking.getTechnician().getUser().getFullName());
    }

    public void notifyCustomerTimeProposal(com.chikchak.model.Booking booking) {
        log.info("Notifying customer {} about time proposal for booking {}",
                booking.getCustomer().getEmail(), booking.getBookingNumber());

        // TODO: Implement actual notification (push, SMS, email)
    }

    public void notifyTechnicianTimeAccepted(Booking booking) {
        log.info("Notifying technician {} that time was accepted for booking {}",
                booking.getTechnician().getUser().getEmail(), booking.getBookingNumber());

        // TODO: Implement actual notification
    }

    public void notifyBookingStatusChanged(com.chikchak.model.Booking booking,
                                           com.chikchak.model.BookingStatus oldStatus,
                                           com.chikchak.model.BookingStatus newStatus) {
        log.info("Notifying parties of status change for booking {} from {} to {}",
                booking.getBookingNumber(), oldStatus, newStatus);

        switch (newStatus) {
            case ON_THE_WAY:
                sendCustomerNotification(booking.getCustomer(),
                        "Your technician is on the way!");
                break;
            case ARRIVED:
                sendCustomerNotification(booking.getCustomer(),
                        "Your technician has arrived. Please confirm to release payment.");
                break;
            case COMPLETED:
                sendCustomerNotification(booking.getCustomer(),
                        "Your service has been completed. Please rate your experience.");
                break;
        }
    }

    public void notifyPaymentRelease(com.chikchak.model.Booking booking) {
        log.info("Processing payment release for booking: {}", booking.getBookingNumber());

        sendTechnicianNotification(booking.getTechnician(), booking,
                "Payment has been released for booking " + booking.getBookingNumber());
    }

    public void notifyBookingCancellation(com.chikchak.model.Booking booking) {
        log.info("Processing cancellation for booking: {}", booking.getBookingNumber());

        if (booking.getTechnician() != null) {
            sendTechnicianNotification(booking.getTechnician(), booking,
                    "Booking " + booking.getBookingNumber() + " has been cancelled");
        }

        sendCustomerNotification(booking.getCustomer(),
                "Your booking has been cancelled. Refund will be processed.");
    }

    private void sendCustomerNotification(com.chikchak.model.User customer, String message) {
        log.info("Sending notification to customer {}: {}", customer.getEmail(), message);
        // TODO: Implement actual push/SMS/email notification
    }

    private void sendTechnicianNotification(com.chikchak.model.TechnicianProfile technician,
                                            com.chikchak.model.Booking booking, String message) {
        log.info("Sending notification to technician {}: {}",
                technician.getUser().getEmail(), message);
        // TODO: Implement actual push/SMS/email notification
    }
}