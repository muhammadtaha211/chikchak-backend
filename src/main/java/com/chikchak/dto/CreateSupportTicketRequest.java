package com.chikchak.dto;

import com.chikchak.model.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSupportTicketRequest {
    @NotBlank(message = "Subject is required")
    private String subject;

    @NotNull(message = "Category is required")
    private TicketCategory category;

    private TicketPriority priority = TicketPriority.MEDIUM;

    @NotBlank(message = "Description is required")
    private String description;

    private ContactMethod preferredContactMethod = ContactMethod.EMAIL;
}