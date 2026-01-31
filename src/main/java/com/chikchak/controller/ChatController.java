package com.chikchak.controller;

import com.chikchak.dto.*;
import com.chikchak.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat and messaging APIs")
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class ChatController {

    private final ChatService chatService;

    // New chat-based endpoints
    @PostMapping("/send")
    @Operation(summary = "Send message", description = "Send a chat message")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(@Valid @RequestBody SendChatMessageRequest request) {
        ChatMessageDto message = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", message));
    }

    @GetMapping("/my-chats")
    @Operation(summary = "Get user's chats", description = "Get all chats for the current user")
    public ResponseEntity<ApiResponse<List<ChatDto>>> getUserChats() {
        List<ChatDto> chats = chatService.getUserChats();
        return ResponseEntity.ok(ApiResponse.success(chats));
    }

    @GetMapping("/{chatId}")
    @Operation(summary = "Get chat details", description = "Get chat details by chat ID")
    public ResponseEntity<ApiResponse<ChatDto>> getChatById(@PathVariable Long chatId) {
        ChatDto chat = chatService.getChatById(chatId);
        return ResponseEntity.ok(ApiResponse.success(chat));
    }

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "Get chat messages", description = "Get all messages in a chat")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<ChatMessageDto> messages;
        if (page == 0 && size == 50) {
            // Get all messages
            messages = chatService.getChatMessages(chatId);
        } else {
            // Get paginated messages
            Pageable pageable = PageRequest.of(page, size);
            messages = chatService.getChatMessages(chatId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/{chatId}/mark-read")
    @Operation(summary = "Mark messages as read", description = "Mark all unread messages in a chat as read")
    public ResponseEntity<ApiResponse<String>> markMessagesAsRead(@PathVariable Long chatId) {
        chatService.markMessagesAsRead(chatId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    @GetMapping("/{chatId}/unread-count")
    @Operation(summary = "Get unread count", description = "Get count of unread messages in a chat")
    public ResponseEntity<ApiResponse<Long>> getUnreadMessageCount(@PathVariable Long chatId) {
        Long count = chatService.getUnreadMessageCount(chatId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // Backward compatibility endpoints - these still work with booking IDs
    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking messages", description = "Get all messages for a booking (legacy)")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getBookingMessages(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<ChatMessageDto> messages;
        if (page == 0 && size == 50) {
            messages = chatService.getBookingMessages(bookingId);
        } else {
            Pageable pageable = PageRequest.of(page, size);
            messages = chatService.getBookingMessages(bookingId, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    // WebSocket endpoints
    @MessageMapping("/chat/{chatId}")
    @SendTo("/topic/chat/{chatId}")
    public ChatMessageDto handleMessage(@DestinationVariable Long chatId, @Payload SendChatMessageRequest request) {
        request.setChatId(chatId);
        return chatService.sendMessage(request);
    }
}