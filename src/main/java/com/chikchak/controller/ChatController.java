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

    @PostMapping("/send")
    @Operation(summary = "Send message", description = "Send a chat message in a booking")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        ChatMessageDto message = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", message));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking messages", description = "Get all messages for a booking")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getBookingMessages(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (page == 0 && size == 50) {
            // Get all messages
            List<ChatMessageDto> messages = chatService.getBookingMessages(bookingId);
            return ResponseEntity.ok(ApiResponse.success(messages));
        } else {
            // Get paginated messages
            Pageable pageable = PageRequest.of(page, size);
            List<ChatMessageDto> messages = chatService.getBookingMessages(bookingId, pageable);
            return ResponseEntity.ok(ApiResponse.success(messages));
        }
    }

    @PostMapping("/booking/{bookingId}/mark-read")
    @Operation(summary = "Mark messages as read", description = "Mark all unread messages in a booking as read")
    public ResponseEntity<ApiResponse<String>> markMessagesAsRead(@PathVariable Long bookingId) {
        chatService.markMessagesAsRead(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    @GetMapping("/booking/{bookingId}/unread-count")
    @Operation(summary = "Get unread count", description = "Get count of unread messages in a booking")
    public ResponseEntity<ApiResponse<Long>> getUnreadMessageCount(@PathVariable Long bookingId) {
        Long count = chatService.getUnreadMessageCount(bookingId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // WebSocket endpoints
    @MessageMapping("/chat/{bookingId}")
    @SendTo("/topic/booking/{bookingId}")
    public ChatMessageDto handleMessage(@DestinationVariable Long bookingId, @Payload SendMessageRequest request) {
        request.setBookingId(bookingId);
        return chatService.sendMessage(request);
    }
}