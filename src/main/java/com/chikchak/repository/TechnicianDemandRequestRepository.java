package com.chikchak.repository;

import com.chikchak.model.DemandStatus;
import com.chikchak.model.ServiceType;
import com.chikchak.model.TechnicianDemandRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicianDemandRequestRepository extends JpaRepository<TechnicianDemandRequest, Long> {

    List<TechnicianDemandRequest> findByUserIdAndStatus(Long userId, DemandStatus status);

    List<TechnicianDemandRequest> findByServiceTypeAndStatus(ServiceType serviceType, DemandStatus status);
}
