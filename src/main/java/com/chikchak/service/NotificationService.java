package com.chikchak.service;

import com.chikchak.model.Booking;
import com.chikchak.model.Chat;
import com.chikchak.model.DeviceToken;
import com.chikchak.model.User;
import com.chikchak.repository.DeviceTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    private static final Locale HEBREW = Locale.forLanguageTag("he");

    //  Notify single technician of new booking
    public void notifyTechnicianOfNewBooking(com.chikchak.model.Booking booking) {
        if (booking.getTechnician() == null) {
            log.warn("Cannot notify technician - no technician assigned to booking {}", booking.getBookingNumber());
            return;
        }

        com.chikchak.model.TechnicianProfile technician = booking.getTechnician();

        log.info("Notifying technician {} of new booking: {}",
                technician.getUser().getEmail(), booking.getBookingNumber());

        String message = String.format(
                "בקשת הזמנה חדשה #%s, %s בכתובת %s, בתאריך %s",
                booking.getBookingNumber(),
                ServiceTypeTranslator.toHebrew(booking.getServiceType()),
                booking.getServiceAddress(),
                booking.getPreferredDatetime().format(DateTimeFormatter.ofPattern("d בMMMM, HH:mm", HEBREW))
        );

        sendPush(technician.getUser(), "בקשת הזמנה חדשה", message,
                bookingData(booking, "NEW_BOOKING_REQUEST"));
    }

    //  Notify customer that technician declined
    public void notifyCustomerBookingDeclined(com.chikchak.model.Booking booking) {
        log.info("Notifying customer {} that booking {} was declined",
                booking.getCustomer().getEmail(), booking.getBookingNumber());

        sendPush(booking.getCustomer(), "ההזמנה נדחתה",
                "בעל המקצוע דחה את ההזמנה. אנא בחר את אופן קבלת ההחזר.",
                bookingData(booking, "BOOKING_DECLINED"));
    }

    public void notifyCustomerBookingAccepted(com.chikchak.model.Booking booking) {
        log.info("Notifying customer {} that booking {} was accepted",
                booking.getCustomer().getEmail(), booking.getBookingNumber());

        sendPush(booking.getCustomer(), "ההזמנה אושרה",
                "ההזמנה שלך אושרה על ידי " + booking.getTechnician().getUser().getFullName(),
                bookingData(booking, "BOOKING_ACCEPTED"));
    }

    public void notifyCustomerTimeProposal(com.chikchak.model.Booking booking) {
        log.info("Notifying customer {} about time proposal for booking {}",
                booking.getCustomer().getEmail(), booking.getBookingNumber());

        sendPush(booking.getCustomer(), "הוצע מועד חדש",
                "בעל המקצוע הציע מועד חדש להזמנה שלך. הקש לצפייה.",
                bookingData(booking, "TIME_PROPOSED"));
    }

    public void notifyTechnicianTimeAccepted(Booking booking) {
        log.info("Notifying technician {} that time was accepted for booking {}",
                booking.getTechnician().getUser().getEmail(), booking.getBookingNumber());

        sendPush(booking.getTechnician().getUser(), "המועד אושר",
                "הלקוח אישר את המועד שהצעת להזמנה " + booking.getBookingNumber(),
                bookingData(booking, "TIME_ACCEPTED"));
    }

    public void notifyBookingStatusChanged(com.chikchak.model.Booking booking,
                                           com.chikchak.model.BookingStatus oldStatus,
                                           com.chikchak.model.BookingStatus newStatus) {
        log.info("Notifying parties of status change for booking {} from {} to {}",
                booking.getBookingNumber(), oldStatus, newStatus);

        switch (newStatus) {
            case ON_THE_WAY:
                sendPush(booking.getCustomer(), "בעל המקצוע בדרך",
                        "בעל המקצוע בדרך אליך!",
                        bookingData(booking, "ON_THE_WAY"));
                break;
            case ARRIVED:
                sendPush(booking.getCustomer(), "בעל המקצוע הגיע",
                        "בעל המקצוע הגיע. אנא אשר את הגעתו לשחרור התשלום.",
                        bookingData(booking, "ARRIVED"));
                break;
            case COMPLETED:
                sendPush(booking.getCustomer(), "השירות הושלם",
                        "השירות הושלם. אנא דרג את החוויה שלך.",
                        bookingData(booking, "COMPLETED"));
                break;
        }
    }

    public void notifyPaymentRelease(com.chikchak.model.Booking booking) {
        log.info("Processing payment release for booking: {}", booking.getBookingNumber());

        if (booking.getTechnician() != null) {
            sendPush(booking.getTechnician().getUser(), "התשלום שוחרר",
                    "התשלום שוחרר עבור הזמנה " + booking.getBookingNumber(),
                    bookingData(booking, "PAYMENT_RELEASED"));
        }
    }

    public void notifyBookingCancellation(com.chikchak.model.Booking booking) {
        log.info("Processing cancellation for booking: {}", booking.getBookingNumber());

        if (booking.getTechnician() != null) {
            sendPush(booking.getTechnician().getUser(), "ההזמנה בוטלה",
                    "הזמנה " + booking.getBookingNumber() + " בוטלה",
                    bookingData(booking, "BOOKING_CANCELLED"));
        }

        sendPush(booking.getCustomer(), "ההזמנה בוטלה",
                "ההזמנה שלך בוטלה. ההחזר יטופל בהקדם.",
                bookingData(booking, "BOOKING_CANCELLED"));
    }

    public void notifyNewChatMessage(User recipient, User sender, Chat chat) {
        log.info("Notifying user {} of new chat message from {}",
                recipient.getEmail(), sender.getEmail());

        Map<String, String> data = new HashMap<>();
        data.put("type", "NEW_MESSAGE");
        data.put("chatId", String.valueOf(chat.getId()));
        if (chat.getBooking() != null) {
            data.put("bookingId", String.valueOf(chat.getBooking().getId()));
        }

        sendPush(recipient, "הודעה חדשה",
                sender.getFullName() + " שלח לך הודעה", data);
    }

    private Map<String, String> bookingData(Booking booking, String type) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        data.put("bookingId", String.valueOf(booking.getId()));
        data.put("bookingNumber", booking.getBookingNumber());
        return data;
    }

    // Sends an FCM push to all of the recipient's devices. Must NEVER throw —
    // a notification failure can't be allowed to break the booking/chat flow.
    private void sendPush(User recipient, String title, String body, Map<String, String> data) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.debug("Firebase not initialized - skipping push to {}", recipient.getEmail());
                return;
            }

            List<DeviceToken> tokens = deviceTokenRepository.findByUserId(recipient.getId());
            if (tokens.isEmpty()) {
                log.debug("No device tokens for user {} - skipping push", recipient.getEmail());
                return;
            }

            for (DeviceToken deviceToken : tokens) {
                sendToToken(deviceToken.getToken(), title, body, data, recipient);
            }
        } catch (Exception e) {
            log.error("Failed to send push notification to {}: {}", recipient.getEmail(), e.getMessage());
        }
    }

    private void sendToToken(String token, String title, String body,
                             Map<String, String> data, User recipient) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("Removing invalid device token for user {} ({})",
                        recipient.getEmail(), e.getMessagingErrorCode());
                deviceTokenRepository.deleteByToken(token);
            } else {
                log.error("FCM send failed for user {}: {}", recipient.getEmail(), e.getMessage());
            }
        }
    }
}
