package com.chikchak.repository;

import com.chikchak.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    Page<User> findByRoleAndStatus(@Param("role") UserRole role, @Param("status") UserStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.accountCredits = u.accountCredits + :amount WHERE u.id = :userId")
    void addAccountCredits(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}

