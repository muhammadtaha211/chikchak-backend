package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Booking Entity
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {
    @Column(name = "booking_number", unique = true, nullable = false)
    private String bookingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private TechnicianProfile technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_category_id", nullable = false)
    private ServiceCategory serviceCategory;

    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingUrgency urgency;

    // Location details
    @Column(name = "service_address", nullable = false)
    private String serviceAddress;

    @Column(name = "service_latitude")
    private Double serviceLatitude;

    @Column(name = "service_longitude")
    private Double serviceLongitude;

    // Scheduling
    @Column(name = "preferred_datetime")
    private LocalDateTime preferredDatetime;

    @Column(name = "scheduled_datetime")
    private LocalDateTime scheduledDatetime;

    @Column(name = "completed_datetime")
    private LocalDateTime completedDatetime;

    // Pricing
    @Column(name = "diagnosis_fee", precision = 10, scale = 2)
    private BigDecimal diagnosisFee;

    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "distance_km", precision = 5, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "distance_fee", precision = 10, scale = 2)
    private BigDecimal distanceFee;

    // Payment
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", columnDefinition = "VARCHAR(255) DEFAULT 'NOT_APPLICABLE'")
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.NOT_APPLICABLE;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_id")
    private String transactionId;

    // Additional fields
    @Column(name = "photos", columnDefinition = "TEXT")
    private String photos; // JSON array of photo URLs

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    @Column(name = "is_emergency")
    @Builder.Default
    private Boolean isEmergency = false;

    @Column(name = "is_rated")
    @Builder.Default
    private Boolean isRated = false;

    @Column(name = "rating_dismissed")
    @Builder.Default
    private Boolean ratingDismissed = false;

    // Relationships
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Chat chat;


    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookingStatusHistory> statusHistory;

    @Column(name = "proposed_datetime")
    private LocalDateTime proposedDatetime; // For time change proposals

    @Column(name = "arrival_marked_at")
    private LocalDateTime arrivalMarkedAt;
}
