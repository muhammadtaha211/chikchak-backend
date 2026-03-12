package com.chikchak.repository;

import com.chikchak.model.Booking;
import com.chikchak.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNumber(String bookingNumber);

    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findByTechnicianId(Long technicianId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByCustomerIdAndStatus(Long customerId, BookingStatus status);

    List<Booking> findByTechnicianIdAndStatus(Long technicianId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId ORDER BY b.createdAt DESC")
    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId ORDER BY b.createdAt DESC")
    Page<Booking> findByTechnicianIdOrderByCreatedAtDesc(@Param("technicianId") Long technicianId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.status IN :statuses")
    List<Booking> findByStatusIn(@Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId AND b.status = 'PENDING'")
    List<Booking> findPendingBookingsForTechnician(@Param("technicianId") Long technicianId);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.status IN ('ACCEPTED', 'SCHEDULED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS')")
    List<Booking> findActiveBookingsForCustomer(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId AND b.status IN ('ACCEPTED', 'SCHEDULED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS')")
    List<Booking> findActiveBookingsForTechnician(@Param("technicianId") Long technicianId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.technician.id = :technicianId AND b.status = 'COMPLETED'")
    Long countCompletedBookingsByTechnicianId(@Param("technicianId") Long technicianId);

    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :startDate AND :endDate")
    List<Booking> findBookingsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<Booking> findByStatusAndArrivalMarkedAtBefore(BookingStatus status, LocalDateTime before);

}
