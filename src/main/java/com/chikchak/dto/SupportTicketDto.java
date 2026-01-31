package com.chikchak.dto;

import com.chikchak.model.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketDto {
    private Long id;
    private String ticketNumber;
    private UserDto user;
    private String subject;
    private TicketCategory category;
    private TicketPriority priority;
    private String description;
    private ContactMethod preferredContactMethod;
    private TicketStatus status;
    private String adminResponse;
    private UserDto assignedAdmin;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime resolvedAt;
}