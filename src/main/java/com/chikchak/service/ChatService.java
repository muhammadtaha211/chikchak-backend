package com.chikchak.service;

import com.chikchak.dto.NotificationDto;
import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.BookingMapper;
import com.chikchak.model.*;
import com.chikchak.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final BookingRepository bookingRepository;
    private final AuthService authService;
    private final BookingMapper bookingMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageDto sendMessage(SendMessageRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        // Validate user can send message to this booking
        validateChatAccess(booking, currentUser);

        ChatMessage message = ChatMessage.builder()
                .booking(booking)
                .sender(currentUser)
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .attachmentUrl(request.getAttachmentUrl())
                .isRead(false)
                .build();

        message = chatMessageRepository.save(message);

        ChatMessageDto messageDto = bookingMapper.toDto(message);

        // Send real-time notification to other party
        sendRealTimeMessage(booking, messageDto, currentUser);

        log.info("Message sent in booking {} by user {}", booking.getBookingNumber(), currentUser.getEmail());

        return messageDto;
    }

    public List<ChatMessageDto> getBookingMessages(Long bookingId, Pageable pageable) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        validateChatAccess(booking, currentUser);

        Page<ChatMessage> messages = chatMessageRepository.findByBookingIdOrderByCreatedAtAsc(bookingId, pageable);

        return messages.getContent().stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto> getBookingMessages(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        validateChatAccess(booking, currentUser);

        List<ChatMessage> messages = chatMessageRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);

        return messages.stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    public void markMessagesAsRead(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        validateChatAccess(booking, currentUser);

        List<ChatMessage> unreadMessages = chatMessageRepository.findByBookingIdAndIsReadFalse(bookingId);

        // Mark messages as read (only messages not sent by current user)
        unreadMessages.stream()
                .filter(msg -> !msg.getSender().getId().equals(currentUser.getId()))
                .forEach(msg -> {
                    msg.setIsRead(true);
                    chatMessageRepository.save(msg);
                });

        log.info("Marked {} messages as read in booking {}",
                unreadMessages.size(), booking.getBookingNumber());
    }

    public Long getUnreadMessageCount(Long bookingId) {
        User currentUser = authService.getCurrentUserEntity();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        validateChatAccess(booking, currentUser);

        return chatMessageRepository.countUnreadMessagesForUser(bookingId, currentUser.getId());
    }

    private void validateChatAccess(Booking booking, User user) {
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
            throw new ChikChakException("Access denied to this chat");
        }
    }

    private void sendRealTimeMessage(Booking booking, ChatMessageDto messageDto, User sender) {
        // Determine recipient
        User recipient = null;
        if (sender.getId().equals(booking.getCustomer().getId())) {
            // Sender is customer, send to technician
            if (booking.getTechnician() != null) {
                recipient = booking.getTechnician().getUser();
            }
        } else if (booking.getTechnician() != null &&
                sender.getId().equals(booking.getTechnician().getUser().getId())) {
            // Sender is technician, send to customer
            recipient = booking.getCustomer();
        }

        if (recipient != null) {
            // Send to specific user's queue
            messagingTemplate.convertAndSendToUser(
                    recipient.getId().toString(),
                    "/queue/messages",
                    messageDto
            );

            // Also send booking update notification
            messagingTemplate.convertAndSendToUser(
                    recipient.getId().toString(),
                    "/queue/notifications",
                    NotificationDto.builder()
                            .type("NEW_MESSAGE")
                            .bookingId(booking.getId())
                            .message("New message in booking " + booking.getBookingNumber())
                            .build()
            );
        }
    }

    public ChatMessageDto sendBookingUpdateMessage(Long bookingId, String updateType, String message) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        // Create system message
        ChatMessage systemMessage = ChatMessage.builder()
                .booking(booking)
                .sender(null) // System message
                .message(message)
                .messageType(MessageType.BOOKING_UPDATE)
                .isRead(false)
                .build();

        systemMessage = chatMessageRepository.save(systemMessage);
        ChatMessageDto messageDto = bookingMapper.toDto(systemMessage);

        // Send to both customer and technician
        messagingTemplate.convertAndSend(
                "/topic/booking/" + bookingId,
                messageDto
        );

        return messageDto;
    }
}