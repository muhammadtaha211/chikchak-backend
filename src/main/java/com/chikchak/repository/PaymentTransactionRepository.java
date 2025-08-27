package com.chikchak.repository;

import com.chikchak.model.PaymentStatus;
import com.chikchak.model.PaymentTransaction;
import com.chikchak.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByBookingId(Long bookingId);

    List<PaymentTransaction> findByUserId(Long userId);

    List<PaymentTransaction> findByStatus(PaymentStatus status);

    List<PaymentTransaction> findByTransactionType(TransactionType transactionType);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.booking.id = :bookingId AND p.transactionType = :type")
    Optional<PaymentTransaction> findByBookingIdAndTransactionType(@Param("bookingId") Long bookingId, @Param("type") TransactionType type);

    @Query("SELECT SUM(p.amount) FROM PaymentTransaction p WHERE p.user.id = :userId AND p.status = 'COMPLETED' AND p.transactionType = 'PAYOUT'")
    Optional<java.math.BigDecimal> getTotalEarningsForTechnician(@Param("userId") Long userId);
}
