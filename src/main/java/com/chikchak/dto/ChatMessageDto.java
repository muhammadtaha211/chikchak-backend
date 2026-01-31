package com.chikchak.dto;

import com.chikchak.dto.UserDto;
import com.chikchak.model.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Updated ChatMessageDto to include chat reference
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long id;
    private Long chatId;
    private UserDto sender;
    private String message;
    private MessageType messageType;
    private Boolean isRead;
    private String attachmentUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}