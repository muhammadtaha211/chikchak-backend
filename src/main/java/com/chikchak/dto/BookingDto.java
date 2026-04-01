package com.chikchak.dto;

import com.chikchak.model.BookingStatus;
import com.chikchak.model.BookingUrgency;
import com.chikchak.model.PaymentStatus;
import com.chikchak.model.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDto {
    private Long id;

    @Builder.Default
    private String bookingNumber = "";

    private UserDto customer;
    private TechnicianProfileDto technician;
    private ServiceCategoryDto serviceCategory;

    @Builder.Default
    private String problemDescription = "";

    private BookingStatus status;
    private BookingUrgency urgency;

    @Builder.Default
    private String serviceAddress = "";



    @Builder.Default
    private Double serviceLatitude = 0.0;

    @Builder.Default
    private Double serviceLongitude = 0.0;

    // REMOVED @JsonFormat — JacksonConfig handles DateTime globally now
    private LocalDateTime preferredDatetime;
    private LocalDateTime proposedDatetime;
    private LocalDateTime scheduledDatetime;
    private LocalDateTime completedDatetime;

    @Builder.Default
    private BigDecimal diagnosisFee = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal distanceKm = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal distanceFee = BigDecimal.ZERO;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.NOT_APPLICABLE;

    @Builder.Default
    private String paymentMethod = "";

    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Builder.Default
    private String customerNotes = "";

    @Builder.Default
    private String technicianNotes = "";

    @Builder.Default
    private Boolean isEmergency = false;

    @Builder.Default
    private Boolean isRated = false;

    @Builder.Default
    private Boolean ratingDismissed = false;

    private LocalDateTime createdAt;

    @Builder.Default
    private Integer unreadMessagesCount = 0;
}