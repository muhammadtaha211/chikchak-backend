package com.chikchak.repository;

import com.chikchak.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<Rating> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Rating> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.technician.id = :technicianId")
    BigDecimal calculateAverageRatingByTechnicianId(@Param("technicianId") Long technicianId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.technician.id = :technicianId")
    Long countByTechnicianId(@Param("technicianId") Long technicianId);
}
