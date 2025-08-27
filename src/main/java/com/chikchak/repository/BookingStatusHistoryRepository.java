package com.chikchak.repository;

import com.chikchak.model.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    @Query("SELECT h FROM BookingStatusHistory h WHERE h.booking.id = :bookingId ORDER BY h.createdAt ASC")
    List<BookingStatusHistory> findBookingStatusHistory(@Param("bookingId") Long bookingId);
}
