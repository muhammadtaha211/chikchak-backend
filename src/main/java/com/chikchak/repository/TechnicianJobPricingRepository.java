package com.chikchak.repository;

import com.chikchak.model.TechnicianJobPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicianJobPricingRepository extends JpaRepository<TechnicianJobPricing, Long> {
    List<TechnicianJobPricing> findByTechnicianIdOrderByJobTypeName(Long technicianId);

    void deleteByTechnicianId(Long technicianId);
}
