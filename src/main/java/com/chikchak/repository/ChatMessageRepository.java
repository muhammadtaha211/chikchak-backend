package com.chikchak.repository;

import com.chikchak.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chat.id = :chatId ORDER BY cm.createdAt ASC")
    Page<ChatMessage> findByChatIdOrderByCreatedAtAsc(@Param("chatId") Long chatId, Pageable pageable);

    List<ChatMessage> findByChatIdAndIsReadFalse(Long chatId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chat.id = :chatId AND cm.sender IS NOT NULL AND cm.sender.id != :userId AND cm.isRead = false")
    Long countUnreadMessagesForUser(@Param("chatId") Long chatId, @Param("userId") Long userId);

    // Mark every unread message in a chat that was NOT sent by the given user as read, in a
    // single UPDATE instead of one save() per message. clearAutomatically keeps the persistence
    // context consistent with the bulk change. Returns the number of rows updated.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessage cm SET cm.isRead = true " +
           "WHERE cm.chat.id = :chatId AND cm.isRead = false " +
           "AND cm.sender IS NOT NULL AND cm.sender.id <> :userId")
    int markMessagesAsRead(@Param("chatId") Long chatId, @Param("userId") Long userId);
}