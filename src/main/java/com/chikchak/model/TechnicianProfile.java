package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// Technician Profile Entity
@Entity
@Table(name = "technician_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianProfile extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "base_diagnosis_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal baseDiagnosisFee;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = new BigDecimal("5.00");

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "total_jobs")
    @Builder.Default
    private Integer totalJobs = 0;

    // Working hours (JSON format or separate entity)
    @Column(name = "working_hours", columnDefinition = "TEXT")
    private String workingHours=""; // JSON: {"monday": {"start": "08:00", "end": "18:00"}, ...}

    // Service radius in kilometers
    @Column(name = "service_radius")
    @Builder.Default
    private Integer serviceRadius = 25;

    // Service category offered
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_category_id")
    private ServiceCategory serviceCategory;

    @OneToMany(mappedBy = "technician", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings;
}
