package com.chikchak.repository;

import com.chikchak.model.AdminActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    Page<AdminActionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
