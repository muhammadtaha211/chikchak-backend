package com.chikchak.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Service Category Entity
@Entity
@Table(name = "service_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "name_he") // Hebrew translation
    private String nameHe;

    @Column(name = "name_ar") // Arabic translation
    private String nameAr;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
