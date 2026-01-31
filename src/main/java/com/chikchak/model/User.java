package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// User Entity
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "he";

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // Location data
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;

    // Account credits for refunds
    @Column(name = "account_credits", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal accountCredits = BigDecimal.ZERO;

    // Relationships
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> customerBookings;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TechnicianProfile technicianProfile;
} 
