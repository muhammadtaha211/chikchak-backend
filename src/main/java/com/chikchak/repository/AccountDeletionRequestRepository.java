package com.chikchak.repository;

import com.chikchak.model.AccountDeletionRequest;
import com.chikchak.model.DeletionRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {

    // Pending queue: oldest first so the operator works through them in order
    Page<AccountDeletionRequest> findByStatusOrderByCreatedAtAsc(DeletionRequestStatus status, Pageable pageable);

    // Full history: newest first
    Page<AccountDeletionRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
