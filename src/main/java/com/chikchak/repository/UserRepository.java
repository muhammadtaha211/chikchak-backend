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
    boolean existsByPhoneNumber(String phoneNumber);
    List<User> findByRole(UserRole role);
    List<User> findByStatus(UserStatus status);

    // Returns a list, not Optional: phone_number is not unique in the schema, so
    // a single-result query could throw NonUniqueResultException (500). Callers
    // must handle the empty and multi-match cases explicitly.
    List<User> findByPhoneNumberAndStatus(String phoneNumber, UserStatus status);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    Page<User> findByRoleAndStatus(@Param("role") UserRole role, @Param("status") UserStatus status, Pageable pageable);

    // search must be non-null (empty string matches all) — binding null into LOWER/CONCAT breaks PostgreSQL type inference
    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdAt DESC")
    Page<User> searchUsersForAdmin(@Param("role") UserRole role,
                                   @Param("status") UserStatus status,
                                   @Param("search") String search,
                                   Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.accountCredits = u.accountCredits + :amount WHERE u.id = :userId")
    void addAccountCredits(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}

