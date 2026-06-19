package com.chikchak.repository;

import com.chikchak.model.Booking;
import com.chikchak.model.BookingStatus;
import com.chikchak.model.RefundStatus;
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

    // Customer dashboard totals — count in the database instead of loading the whole booking
    // history into memory just to size it.
    long countByCustomerId(Long customerId);

    long countByCustomerIdAndStatus(Long customerId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId ORDER BY b.createdAt DESC")
    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId ORDER BY b.createdAt DESC")
    Page<Booking> findByTechnicianIdOrderByCreatedAtDesc(@Param("technicianId") Long technicianId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId AND b.status IN :statuses ORDER BY b.createdAt DESC")
    Page<Booking> findByTechnicianIdAndStatusInOrderByCreatedAtDesc(@Param("technicianId") Long technicianId,
                                                                   @Param("statuses") List<BookingStatus> statuses,
                                                                   Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.status IN :statuses")
    List<Booking> findByStatusIn(@Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId AND b.status = 'PENDING'")
    List<Booking> findPendingBookingsForTechnician(@Param("technicianId") Long technicianId);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND (b.status IN ('PENDING', 'TECHNICIAN_NOTIFIED', 'TIME_DISCUSSION', 'ACCEPTED', 'SCHEDULED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'ASSESSMENT_COMPLETE') OR (b.status = 'COMPLETED' AND b.isRated = false AND b.ratingDismissed = false))")
    List<Booking> findActiveBookingsForCustomer(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.status IN ('PENDING', 'TECHNICIAN_NOTIFIED', 'TIME_DISCUSSION', 'ACCEPTED', 'SCHEDULED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'ASSESSMENT_COMPLETE')")
    List<Booking> findNonTerminalBookingsForCustomer(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId AND b.status IN ('ACCEPTED', 'SCHEDULED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS')")
    List<Booking> findActiveBookingsForTechnician(@Param("technicianId") Long technicianId);

    @Query("SELECT b FROM Booking b WHERE b.technician.id = :technicianId AND b.status IN ('PENDING', 'TECHNICIAN_NOTIFIED', 'TIME_DISCUSSION', 'ACCEPTED', 'SCHEDULED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'ASSESSMENT_COMPLETE')")
    List<Booking> findNonTerminalBookingsForTechnician(@Param("technicianId") Long technicianId);

    // Non-terminal bookings between two users (one the customer, the other the technician's user),
    // in either arrangement. DISPUTED is excluded so blocking can't short-circuit dispute resolution.
    // Used to auto-cancel active bookings when one party blocks the other.
    @Query("SELECT b FROM Booking b WHERE b.status NOT IN ('CANCELLED', 'COMPLETED', 'DISPUTED') " +
            "AND ((b.customer.id = :a AND b.technician.user.id = :b) " +
            "OR (b.customer.id = :b AND b.technician.user.id = :a))")
    List<Booking> findActiveBookingsBetweenUsers(@Param("a") Long a, @Param("b") Long b);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.technician.id = :technicianId AND b.status = 'COMPLETED'")
    Long countCompletedBookingsByTechnicianId(@Param("technicianId") Long technicianId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.technician.id = :technicianId AND b.status = 'PENDING'")
    Long countNewRequestsForTechnician(@Param("technicianId") Long technicianId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.technician.id = :technicianId AND b.status IN ('ACCEPTED', 'SCHEDULED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS')")
    Long countConfirmedBookingsForTechnician(@Param("technicianId") Long technicianId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.technician.id = :technicianId AND b.status = 'TIME_DISCUSSION'")
    Long countTimeDiscussionForTechnician(@Param("technicianId") Long technicianId);

    // Technician badge counts in a single grouped query instead of one COUNT per status.
    // Returns [status, count] rows for the requested statuses.
    @Query("SELECT b.status, COUNT(b) FROM Booking b " +
           "WHERE b.technician.id = :technicianId AND b.status IN :statuses " +
           "GROUP BY b.status")
    List<Object[]> countByTechnicianIdGroupedByStatus(@Param("technicianId") Long technicianId,
                                                       @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :startDate AND :endDate")
    List<Booking> findBookingsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<Booking> findByStatusAndArrivalMarkedAtBefore(BookingStatus status, LocalDateTime before);

    List<Booking> findByCustomerIdAndRefundStatus(Long customerId, RefundStatus refundStatus);

    List<Booking> findByStatusInAndCreatedAtBefore(List<BookingStatus> statuses, LocalDateTime before);

    List<Booking> findByStatusAndProposedDatetimeBefore(BookingStatus status, LocalDateTime before);

    // search must be non-null (empty string matches all) — binding null into LOWER/CONCAT breaks PostgreSQL type inference
    @Query("SELECT b FROM Booking b WHERE (:status IS NULL OR b.status = :status) " +
            "AND (LOWER(b.bookingNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY b.createdAt DESC")
    Page<Booking> searchBookingsForAdmin(@Param("status") BookingStatus status,
                                         @Param("search") String search,
                                         Pageable pageable);
}
