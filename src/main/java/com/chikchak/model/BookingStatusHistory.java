package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Booking Status History Entity
@Entity
@Table(name = "booking_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private BookingStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private BookingStatus newStatus;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}
