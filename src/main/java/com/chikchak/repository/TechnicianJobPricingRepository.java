package com.chikchak.repository;

import com.chikchak.model.TechnicianJobPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianJobPricingRepository extends JpaRepository<TechnicianJobPricing, Long> {
    List<TechnicianJobPricing> findByTechnicianIdOrderByJobTypeName(Long technicianId);

    List<TechnicianJobPricing> findByTechnicianIdAndIsOfferedTrueOrderByJobTypeName(Long technicianId);

    Optional<TechnicianJobPricing> findByTechnicianIdAndJobTypeId(Long technicianId, Long jobTypeId);

    void deleteByTechnicianId(Long technicianId);
}
