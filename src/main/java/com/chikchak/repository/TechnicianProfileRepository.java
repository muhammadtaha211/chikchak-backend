package com.chikchak.repository;

import com.chikchak.model.ServiceType;
import com.chikchak.model.TechnicianProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile, Long> {
    Optional<TechnicianProfile> findByUserId(Long userId);

    List<TechnicianProfile> findByIsVerifiedAndIsAvailable(Boolean isVerified, Boolean isAvailable);

    @Query("SELECT t FROM TechnicianProfile t JOIN t.services s WHERE s.id = :serviceId AND t.isVerified = true AND t.isAvailable = true")
    List<TechnicianProfile> findAvailableTechniciansByService(@Param("serviceId") Long serviceId);

    @Query("SELECT t FROM TechnicianProfile t JOIN t.services s WHERE s.serviceType = :serviceType AND t.isVerified = true AND t.isAvailable = true")
    List<TechnicianProfile> findAvailableTechniciansByServiceType(@Param("serviceType") ServiceType serviceType);

    @Query("SELECT t FROM TechnicianProfile t WHERE t.isVerified = true AND t.isAvailable = true " +
            "AND (:latitude IS NULL OR :longitude IS NULL OR " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(t.user.latitude)) * " +
            "cos(radians(t.user.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(t.user.latitude)))) <= t.serviceRadius)")
    List<TechnicianProfile> findTechniciansWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );

    @Query("SELECT t FROM TechnicianProfile t JOIN t.services s " +
            "WHERE s.id = :serviceId AND t.isVerified = true AND t.isAvailable = true " +
            "AND (:latitude IS NULL OR :longitude IS NULL OR " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(t.user.latitude)) * " +
            "cos(radians(t.user.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(t.user.latitude)))) <= t.serviceRadius) " +
            "ORDER BY t.averageRating DESC, t.totalJobs DESC")
    List<TechnicianProfile> findBestTechniciansForServiceAndLocation(
            @Param("serviceId") Long serviceId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );

    @Query("SELECT t FROM TechnicianProfile t JOIN t.services s " +
            "WHERE s.serviceType = :serviceType AND t.isVerified = true AND t.isAvailable = true " +
            "AND (:latitude IS NULL OR :longitude IS NULL OR " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(t.user.latitude)) * " +
            "cos(radians(t.user.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(t.user.latitude)))) <= t.serviceRadius) " +
            "ORDER BY t.averageRating DESC, t.totalJobs DESC")
    List<TechnicianProfile> findBestTechniciansForServiceTypeAndLocation(
            @Param("serviceType") ServiceType serviceType,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );
}
