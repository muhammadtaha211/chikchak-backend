package com.chikchak.service;

import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final TechnicianProfileRepository technicianProfileRepository;
    private final AuthService authService;
    private final BookingMapper bookingMapper;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageDto sendMessage(SendChatMessageRequest request) {
        User currentUser = authService.getCurrentUserEntity();

        Chat chat = findOrCreateChat(request.getChatId(), request.getCustomerId(), request.getTechnicianId(), request.getBookingId());

        // Validate user can send message to this chat
        validateChatAccess(chat, currentUser);

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

        List<ChatMessage> unreadMessages = chatMessageRepository.findByChatIdAndIsReadFalse(chatId);

        // Mark messages as read (only messages not sent by current user)
        unreadMessages.stream()
                .filter(msg -> msg.getSender() != null && !msg.getSender().getId().equals(currentUser.getId()))
                .forEach(msg -> {
                    msg.setIsRead(true);
                    chatMessageRepository.save(msg);
                });

        log.info("Marked {} messages as read in chat {}",
                unreadMessages.size(), chat.getId());
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

        return chats.stream()
                .map(this::toChatDto)
                .collect(Collectors.toList());
    }

    public List<ChatDto> getActiveChatsForUser() {
        User currentUser = authService.getCurrentUserEntity();
        List<Chat> chats = chatRepository.findActiveChatsForUser(currentUser.getId());

        return chats.stream()
                .map(this::toChatDto)
                .collect(Collectors.toList());
    }

    public ChatDto getChatById(Long chatId) {
        User currentUser = authService.getCurrentUserEntity();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChikChakException("Chat not found"));

        validateChatAccess(chat, currentUser);

        return toChatDto(chat);
    }

    private ChatDto toChatDto(Chat chat) {
        User currentUser = authService.getCurrentUserEntity();

        // Determine the other participant
        User otherParticipant = chat.getCustomer().getId().equals(currentUser.getId())
                ? chat.getTechnician()
                : chat.getCustomer();

        // Get unread count for current user
        Long unreadCount = chatRepository.countUnreadMessagesInChat(chat.getId(), currentUser.getId());

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
                        .message("New message from " + sender.getFullName())
                        .data(Map.of("chatId", chat.getId()))
                        .build()
        );
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