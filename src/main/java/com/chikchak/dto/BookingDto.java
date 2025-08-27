package com.chikchak.dto;

import com.chikchak.model.BookingStatus;
import com.chikchak.model.BookingUrgency;
import com.chikchak.model.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDto {
    private Long id;
    private String bookingNumber;
    private UserDto customer;
    private TechnicianProfileDto technician;
    private ServiceCategoryDto serviceCategory;
    private String problemDescription;
    private BookingStatus status;
    private BookingUrgency urgency;

    private String serviceAddress;
    private String serviceCity;
    private Double serviceLatitude;
    private Double serviceLongitude;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime preferredDatetime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledDatetime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedDatetime;

    private BigDecimal diagnosisFee;
    private BigDecimal serviceFee;
    private BigDecimal totalAmount;
    private BigDecimal distanceKm;
    private BigDecimal distanceFee;

    private PaymentStatus paymentStatus;
    private String paymentMethod;

    private List<String> photos;
    private String customerNotes;
    private String technicianNotes;
    private Boolean isEmergency;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private Integer unreadMessagesCount;
}
