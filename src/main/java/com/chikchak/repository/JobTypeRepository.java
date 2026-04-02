package com.chikchak.repository;

import com.chikchak.model.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobTypeRepository extends JpaRepository<JobType, Long> {
    List<JobType> findByServiceCategoryIdOrderByName(Long serviceCategoryId);
}
