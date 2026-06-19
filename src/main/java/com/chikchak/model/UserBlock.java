package com.chikchak.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// A one-directional block: `blocker` no longer wants to interact with `blocked`.
// A block in either direction prevents the two users from messaging each other.
@Entity
@Table(name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_block", columnNames = {"blocker_id", "blocked_id"}),
        indexes = {
                @Index(name = "idx_user_blocks_blocker", columnList = "blocker_id"),
                @Index(name = "idx_user_blocks_blocked", columnList = "blocked_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;
}
