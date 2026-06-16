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

    // Find all chats for a user (as customer or technician).
    // Fetch-joins every to-one association the chat-list DTO mapping reads (participants and,
    // when present, the booking's customer + technician + technician.user) so building the list
    // is a single query instead of several lazy loads per chat. All joins are to-one, so there
    // is no row multiplication.
    @Query("SELECT c FROM Chat c " +
           "JOIN FETCH c.customer " +
           "JOIN FETCH c.technician " +
           "LEFT JOIN FETCH c.booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.technician bt " +
           "LEFT JOIN FETCH bt.user " +
           "WHERE c.customer.id = :userId OR c.technician.id = :userId " +
           "ORDER BY c.lastMessageAt DESC")
    List<Chat> findChatsForUser(@Param("userId") Long userId);

    // Find active chats for a user (same projection as findChatsForUser)
    @Query("SELECT c FROM Chat c " +
           "JOIN FETCH c.customer " +
           "JOIN FETCH c.technician " +
           "LEFT JOIN FETCH c.booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.technician bt " +
           "LEFT JOIN FETCH bt.user " +
           "WHERE (c.customer.id = :userId OR c.technician.id = :userId) " +
           "ORDER BY c.lastMessageAt DESC")
    List<Chat> findActiveChatsForUser(@Param("userId") Long userId);

    // Find chat between specific customer and technician
    @Query("SELECT c FROM Chat c WHERE c.customer.id = :customerId AND c.technician.id = :technicianId AND c.booking IS NULL")
    Optional<Chat> findPreBookingChat(@Param("customerId") Long customerId, @Param("technicianId") Long technicianId);

    // Find chat for specific booking
    Optional<Chat> findByBookingId(Long bookingId);

    // Count unread messages in a chat for a specific user
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chat.id = :chatId AND cm.sender IS NOT NULL AND cm.sender.id != :userId AND cm.isRead = false")
    Long countUnreadMessagesInChat(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Count unread messages for a user, addressed by booking id (joins chat -> booking)
    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.chat.booking.id = :bookingId " +
           "AND cm.sender IS NOT NULL " +
           "AND cm.sender.id != :userId " +
           "AND cm.isRead = false")
    Long countUnreadMessagesByBookingId(@Param("bookingId") Long bookingId,
                                        @Param("userId") Long userId);

    // Batched version: returns [bookingId, count] rows for the given booking ids in a single query
    @Query("SELECT cm.chat.booking.id, COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.chat.booking.id IN :bookingIds " +
           "AND cm.sender IS NOT NULL " +
           "AND cm.sender.id != :userId " +
           "AND cm.isRead = false " +
           "GROUP BY cm.chat.booking.id")
    List<Object[]> countUnreadMessagesByBookingIds(@Param("bookingIds") List<Long> bookingIds,
                                                   @Param("userId") Long userId);

    // Batched unread counts keyed by chat id — one query for a whole chat list instead of one
    // COUNT per chat (the chat-list N+1). Returns [chatId, count] rows.
    @Query("SELECT cm.chat.id, COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.chat.id IN :chatIds " +
           "AND cm.sender IS NOT NULL " +
           "AND cm.sender.id != :userId " +
           "AND cm.isRead = false " +
           "GROUP BY cm.chat.id")
    List<Object[]> countUnreadMessagesByChatIds(@Param("chatIds") List<Long> chatIds,
                                                @Param("userId") Long userId);
}