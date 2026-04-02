package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobType extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "name_he")
    private String nameHe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_category_id", nullable = false)
    private ServiceCategory serviceCategory;
}
