package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Dormant: platform pricing now lives on JobType.minPrice/maxPrice. Kept for potential re-introduction of per-technician pricing later.
@Deprecated
@Entity
@Table(name = "technician_job_pricing",
        uniqueConstraints = @UniqueConstraint(columnNames = {"technician_id", "job_type_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianJobPricing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", nullable = false)
    private TechnicianProfile technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_type_id", nullable = false)
    private JobType jobType;

    @Column(name = "min_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal maxPrice;

    @Column(name = "is_offered")
    @Builder.Default
    private Boolean isOffered = true;
}
