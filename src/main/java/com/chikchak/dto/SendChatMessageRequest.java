package com.chikchak.dto;

import com.chikchak.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Enhanced message request for chat-based messaging
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendChatMessageRequest {
    // For existing chats
    private Long chatId;

    // For booking-based chats
    private Long bookingId;

    // For creating new pre-booking chats
    private Long customerId;
    private Long technicianId;

    @NotBlank(message = "Message is required")
    private String message;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    private String attachmentUrl;
}