package com.chikchak.repository;

import com.chikchak.model.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    List<UserBlock> findByBlocker_Id(Long blockerId);

    void deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    // All user ids that have a block relationship with :uid (either direction).
    // Used to hide blocked conversations from the chat list.
    @Query("select case when b.blocker.id = :uid then b.blocked.id else b.blocker.id end " +
            "from UserBlock b where b.blocker.id = :uid or b.blocked.id = :uid")
    List<Long> findRelatedUserIds(@Param("uid") Long uid);

    // True if either user has blocked the other — used to reject messaging.
    @Query("select count(b) > 0 from UserBlock b where " +
            "(b.blocker.id = :a and b.blocked.id = :b) or (b.blocker.id = :b and b.blocked.id = :a)")
    boolean existsBlockBetween(@Param("a") Long a, @Param("b") Long b);
}
