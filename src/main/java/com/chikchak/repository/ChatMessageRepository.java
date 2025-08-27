package com.chikchak.repository;

import com.chikchak.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    List<ChatMessage> findByBookingIdAndIsReadFalse(Long bookingId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.booking.id = :bookingId AND m.sender.id != :userId AND m.isRead = false")
    Long countUnreadMessagesForUser(@Param("bookingId") Long bookingId, @Param("userId") Long userId);

    @Query("SELECT m FROM ChatMessage m WHERE m.booking.id = :bookingId ORDER BY m.createdAt ASC")
    Page<ChatMessage> findByBookingIdOrderByCreatedAtAsc(@Param("bookingId") Long bookingId, Pageable pageable);
}
