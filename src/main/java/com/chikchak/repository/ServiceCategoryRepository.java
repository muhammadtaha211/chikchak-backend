package com.chikchak.repository;

import com.chikchak.model.ServiceCategory;
import com.chikchak.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findByIsActiveOrderByName(Boolean isActive);

    Optional<ServiceCategory> findByNameIgnoreCase(String name);

    Optional<ServiceCategory> findByServiceType(ServiceType serviceType);
}
