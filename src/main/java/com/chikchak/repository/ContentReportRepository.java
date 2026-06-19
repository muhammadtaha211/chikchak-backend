package com.chikchak.repository;

import com.chikchak.model.ContentReport;
import com.chikchak.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    Page<ContentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Oldest-first when filtering (e.g. status=OPEN) so the admin works the queue in order.
    Page<ContentReport> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);
}
