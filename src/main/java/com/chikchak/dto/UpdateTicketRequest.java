package com.chikchak.dto;

import com.chikchak.model.*;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketRequest {
    private TicketStatus status;
    private String adminResponse;
    private Long assignedAdminId;
}