package com.chikchak.dto;

import com.chikchak.model.BookingUrgency;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// Booking DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {
    @NotNull(message = "Service category is required")
    private Long serviceCategoryId;

    @NotBlank(message = "Problem description is required")
    private String problemDescription;

    @NotBlank(message = "Service address is required")
    private String serviceAddress;



    @NotNull(message = "Service location coordinates are required")
    private Double serviceLatitude;

    @NotNull(message = "Service location coordinates are required")
    private Double serviceLongitude;

    @JsonFormat(pattern = "yyyy-MM-dd['T'][' ']HH:mm:ss[.SSS]")
    private LocalDateTime preferredDatetime;

    @NotNull(message = "Urgency level is required")
    private BookingUrgency urgency = BookingUrgency.NORMAL;

    private List<String> photos;
    private String customerNotes;
    private Boolean isEmergency = false;



    @NotNull(message = "Technician selection is required")
    private Long selectedTechnicianId; // Which technician customer selected

    @Builder.Default
    private Boolean useAccountCredits = false; // Use credits for payment

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // "CREDIT_CARD", "DEBIT_CARD", etc.

}
