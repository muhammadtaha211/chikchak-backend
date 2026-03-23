package com.chikchak.repository;

import com.chikchak.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    // Find all chats for a user (as customer or technician)
    @Query("SELECT c FROM Chat c WHERE c.customer.id = :userId OR c.technician.id = :userId ORDER BY c.lastMessageAt DESC")
    List<Chat> findChatsForUser(@Param("userId") Long userId);

    // Find active chats for a user
    @Query("SELECT c FROM Chat c WHERE (c.customer.id = :userId OR c.technician.id = :userId) ORDER BY c.lastMessageAt DESC")
    List<Chat> findActiveChatsForUser(@Param("userId") Long userId);

    // Find chat between specific customer and technician
    @Query("SELECT c FROM Chat c WHERE c.customer.id = :customerId AND c.technician.id = :technicianId AND c.booking IS NULL")
    Optional<Chat> findPreBookingChat(@Param("customerId") Long customerId, @Param("technicianId") Long technicianId);

    // Find chat for specific booking
    Optional<Chat> findByBookingId(Long bookingId);

    // Count unread messages in a chat for a specific user
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chat.id = :chatId AND cm.sender IS NOT NULL AND cm.sender.id != :userId AND cm.isRead = false")
    Long countUnreadMessagesInChat(@Param("chatId") Long chatId, @Param("userId") Long userId);
}