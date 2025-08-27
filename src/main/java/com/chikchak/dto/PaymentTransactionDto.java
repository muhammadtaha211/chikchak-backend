package com.chikchak.dto;

import com.chikchak.model.PaymentStatus;
import com.chikchak.model.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionDto {
    private Long id;
    private Long bookingId;
    private UserDto user;
    private BigDecimal amount;
    private TransactionType transactionType;
    private PaymentStatus status;
    private String paymentMethod;
    private String externalTransactionId;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;
}
