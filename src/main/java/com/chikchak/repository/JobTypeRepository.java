package com.chikchak.repository;

import com.chikchak.model.JobType;
import com.chikchak.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobTypeRepository extends JpaRepository<JobType, Long> {
    List<JobType> findByServiceTypeOrderByMinPriceAscMaxPriceAsc(ServiceType serviceType);
}
