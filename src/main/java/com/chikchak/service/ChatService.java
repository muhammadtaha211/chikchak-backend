package com.chikchak.service;

import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
import com.chikchak.exception.RateLimitExceededException;
import com.chikchak.mapper.BookingMapper;
import com.chikchak.mapper.UserMapper;
import com.chikchak.model.*;
import com.chikchak.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final AuthService authService;
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RateLimitService rateLimitService;
    private final NotificationService notificationService;

    public ChatMessageDto sendMessage(SendChatMessageRequest request) {
        boolean hasMessage = request.getMessage() != null && !request.getMessage().trim().isEmpty();
        boolean hasAttachment = request.getAttachmentUrl() != null && !request.getAttachmentUrl().trim().isEmpty();
        if (!hasMessage && !hasAttachment) {
            throw new ChikChakException("Message or attachment is required");
        }

        User currentUser = authService.getCurrentUserEntity();

        if (!rateLimitService.tryConsume(currentUser.getId())) {
            log.warn("Rate limit exceeded for user {} on chat send", currentUser.getEmail());
            throw new RateLimitExceededException("Rate limit exceeded. Please slow down.");
        }

        Chat chat = findOrCreateChat(request.getChatId(), request.getCustomerId(), request.getTechnicianId(), request.getBookingId());

        // Validate user can send message to this chat
        validateChatAccess(chat, currentUser);

        // Reject if either party has blocked the other (admins are exempt — they moderate)
        if (currentUser.getRole() != UserRole.ADMIN) {
            Long otherId = otherParticipantId(chat, currentUser);
            if (userBlockRepository.existsBlockBetween(currentUser.getId(), otherId)) {
                throw new ChikChakException("You can no longer message this user because one of you has blocked the other.");
            }
        }

        ChatMessage message = ChatMessage.builder()
                .chat(chat)
                .sender(currentUser)
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .attachmentUrl(request.getAttachmentUrl())
                .isRead(false)
                .build();

        message = chatMessageRepository.save(message);

        // Update chat's last message info
        chat.updateLastMessage(message);
        chatRepository.save(chat);

        ChatMessageDto messageDto = bookingMapper.toDto(message);

        // Send real-time notification to other party
        sendRealTimeMessage(chat, messageDto, currentUser);

        log.info("Message sent in chat {} by user {}", chat.getId(), currentUser.getEmail());

        return messageDto;
    }

    public ChatMessageDto sendMessage(SendMessageRequest request) {
        // For backward compatibility - convert old request to new format
        SendChatMessageRequest newRequest = SendChatMessageRequest.builder()
                .bookingId(request.getBookingId())
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .attachmentUrl(request.getAttachmentUrl())
                .build();

        return sendMessage(newRequest);
    }

    private Chat findOrCreateChat(Long chatId, Long customerId, Long technicianId, Long bookingId) {
        // If chatId is provided, find existing chat
        if (chatId != null) {
            return chatRepository.findById(chatId)
                    .orElseThrow(() -> new ChikChakException("Chat not found"));
        }

        // If bookingId is provided, find or create chat for that booking
        if (bookingId != null) {
            Optional<Chat> existingChat = chatRepository.findByBookingId(bookingId);
            if (existingChat.isPresent()) {
                return existingChat.get();
            }

            // Create new chat for booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ChikChakException("Booking not found"));

            return createChatForBooking(booking);
        }

        // For pre-booking chat, find existing or create new
        if (customerId != null && technicianId != null) {
            Optional<Chat> existingChat = chatRepository.findPreBookingChat(customerId, technicianId);
            if (existingChat.isPresent()) {
                return existingChat.get();
            }

            // Create new pre-booking chat
            User customer = userRepository.findById(customerId)
                    .orElseThrow(() -> new ChikChakException("Customer not found"));
            User technician = userRepository.findById(technicianId)
                    .orElseThrow(() -> new ChikChakException("Technician not found"));

            return createPreBookingChat(customer, technician);
        }

        throw new ChikChakException("Insufficient information to find or create chat");
    }

    private Chat createChatForBooking(Booking booking) {
        User technician = booking.getTechnician() != null ? booking.getTechnician().getUser() : null;
        if (technician == null) {
            throw new ChikChakException("Booking must have assigned technician to create chat");
        }

        Chat chat = Chat.builder()
                .customer(booking.getCustomer())
                .technician(technician)
                .booking(booking)
                .build();

        return chatRepository.save(chat);
    }

    private Chat createPreBookingChat(User customer, User technician) {
        Chat chat = Chat.builder()
                .customer(customer)
                .technician(technician)
                .booking(null)
                .build();

        return chatRepository.save(chat);
    }

    public List<ChatMessageDto> getChatMessages(Long chatId, Pageable pageable) {
        User currentUser = authService.getCurrentUserEntity();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChikChakException("Chat not found"));

        validateChatAccess(chat, currentUser);

        if (pageable != null) {
            Page<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId, pageable);
            return messages.getContent().stream()
                    .map(bookingMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
            return messages.stream()
                    .map(bookingMapper::toDto)
                    .collect(Collectors.toList());
        }
    }

    public List<ChatMessageDto> getChatMessages(Long chatId) {
        return getChatMessages(chatId, null);
    }

    // For backward compatibility with booking-based message retrieval
    public List<ChatMessageDto> getBookingMessages(Long bookingId, Pageable pageable) {
        Chat chat = chatRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ChikChakException("No chat found for booking"));

        return getChatMessages(chat.getId(), pageable);
    }

    public List<ChatMessageDto> getBookingMessages(Long bookingId) {
        return getBookingMessages(bookingId, null);
    }

    public void markMessagesAsRead(Long chatId) {
        User currentUser = authService.getCurrentUserEntity();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChikChakException("Chat not found"));

        validateChatAccess(chat, currentUser);

        // Single bulk UPDATE instead of one save() per unread message.
        int updated = chatMessageRepository.markMessagesAsRead(chatId, currentUser.getId());

        log.info("Marked {} messages as read in chat {}", updated, chat.getId());
    }

    public Long getUnreadMessageCount(Long chatId) {
        User currentUser = authService.getCurrentUserEntity();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChikChakException("Chat not found"));

        validateChatAccess(chat, currentUser);

        return chatMessageRepository.countUnreadMessagesForUser(chatId, currentUser.getId());
    }

    public List<ChatDto> getUserChats() {
        User currentUser = authService.getCurrentUserEntity();
        List<Chat> chats = chatRepository.findChatsForUser(currentUser.getId());

        // Hide conversations with users blocked in either direction
        Set<Long> blockedRelated = new HashSet<>(userBlockRepository.findRelatedUserIds(currentUser.getId()));
        if (!blockedRelated.isEmpty()) {
            chats = chats.stream()
                    .filter(chat -> !blockedRelated.contains(otherParticipantId(chat, currentUser)))
                    .collect(Collectors.toList());
        }

        return toChatDtos(chats, currentUser);
    }

    public List<ChatDto> getActiveChatsForUser() {
        User currentUser = authService.getCurrentUserEntity();
        return toChatDtos(chatRepository.findActiveChatsForUser(currentUser.getId()), currentUser);
    }

    public ChatDto getChatById(Long chatId) {
        User currentUser = authService.getCurrentUserEntity();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChikChakException("Chat not found"));

        validateChatAccess(chat, currentUser);

        Long unreadCount = chatRepository.countUnreadMessagesInChat(chat.getId(), currentUser.getId());
        return toChatDto(chat, currentUser, unreadCount);
    }

    // Maps a list of chats with their unread counts resolved in ONE batched query (keyed by
    // chat id) instead of a COUNT per chat — this was the chat-list N+1.
    private List<ChatDto> toChatDtos(List<Chat> chats, User currentUser) {
        if (chats.isEmpty()) {
            return List.of();
        }
        List<Long> chatIds = chats.stream().map(Chat::getId).collect(Collectors.toList());
        Map<Long, Long> unreadByChat = chatRepository
                .countUnreadMessagesByChatIds(chatIds, currentUser.getId()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return chats.stream()
                .map(chat -> toChatDto(chat, currentUser, unreadByChat.getOrDefault(chat.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private ChatDto toChatDto(Chat chat, User currentUser, Long unreadCount) {
        // Determine the other participant
        User otherParticipant = chat.getCustomer().getId().equals(currentUser.getId())
                ? chat.getTechnician()
                : chat.getCustomer();

        return ChatDto.builder()
                .id(chat.getId())
                .customer(userMapper.toDto(chat.getCustomer()))
                .technician(userMapper.toDto(chat.getTechnician()))
                .booking(chat.getBooking() != null ? bookingMapper.toDto(chat.getBooking()) : null)
                .lastMessageAt(chat.getLastMessageAt())
                .lastMessagePreview(chat.getLastMessagePreview())
                .unreadCount(unreadCount)
                .otherParticipant(userMapper.toDto(otherParticipant))
                .createdAt(chat.getCreatedAt())
                .build();
    }

    // Id of the participant in this chat who is NOT the given user
    private Long otherParticipantId(Chat chat, User user) {
        return chat.getCustomer().getId().equals(user.getId())
                ? chat.getTechnician().getId()
                : chat.getCustomer().getId();
    }

    private void validateChatAccess(Chat chat, User user) {
        boolean hasAccess = false;

        switch (user.getRole()) {
            case ADMIN:
                hasAccess = true;
                break;
            case CUSTOMER:
                hasAccess = chat.getCustomer().getId().equals(user.getId());
                break;
            case TECHNICIAN:
                hasAccess = chat.getTechnician().getId().equals(user.getId());
                break;
        }

        if (!hasAccess) {
            throw new ChikChakException("Access denied to this chat");
        }
    }

    private void sendRealTimeMessage(Chat chat, ChatMessageDto messageDto, User sender) {
        // Determine recipient
        User recipient = chat.getCustomer().getId().equals(sender.getId())
                ? chat.getTechnician()
                : chat.getCustomer();

        // Send to specific user's queue
        messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/queue/messages",
                messageDto
        );

        // Also send chat update notification
        messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/queue/notifications",
                NotificationDto.builder()
                        .type("NEW_MESSAGE")
                        .bookingId(chat.getBooking() != null ? chat.getBooking().getId() : null)
                        .message("הודעה חדשה מאת " + sender.getFullName())
                        .data(Map.of("chatId", chat.getId()))
                        .build()
        );

        // FCM push to the other party's devices
        notificationService.notifyNewChatMessage(recipient, sender, chat);
    }

    public ChatMessageDto sendBookingUpdateMessage(Long bookingId, String updateType, String message) {
        Chat chat = chatRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    // Create chat for booking if it doesn't exist
                    Booking booking = bookingRepository.findById(bookingId)
                            .orElseThrow(() -> new ChikChakException("Booking not found"));
                    return createChatForBooking(booking);
                });

        // Create system message
        ChatMessage systemMessage = ChatMessage.builder()
                .chat(chat)
                .sender(null) // System message
                .message(message)
                .messageType(MessageType.BOOKING_UPDATE)
                .isRead(false)
                .build();

        systemMessage = chatMessageRepository.save(systemMessage);

        // Update chat's last message info
        chat.updateLastMessage(systemMessage);
        chatRepository.save(chat);

        ChatMessageDto messageDto = bookingMapper.toDto(systemMessage);

        // Send to both customer and technician
        messagingTemplate.convertAndSend(
                "/topic/chat/" + chat.getId(),
                messageDto
        );

        return messageDto;
    }

    public Chat findOrCreateChatForBooking(Long bookingId) {
        Optional<Chat> existingChat = chatRepository.findByBookingId(bookingId);
        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));

        return createChatForBooking(booking);
    }

}