package com.chikchak.service;

import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.UserMapper;
import com.chikchak.model.*;
import com.chikchak.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceCategoryService {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final UserMapper userMapper;

    public List<ServiceCategoryDto> getActiveServices() {
        return serviceCategoryRepository.findByIsActiveOrderByName(true)
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ServiceCategoryDto> getAllServices() {
        return serviceCategoryRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public ServiceCategoryDto createService(ServiceCategoryDto serviceDto) {
        if (serviceCategoryRepository.findByNameIgnoreCase(serviceDto.getName()).isPresent()) {
            throw new ChikChakException("Service category with this name already exists");
        }

        if (serviceDto.getServiceType() == null) {
            throw new ChikChakException("Service type is required");
        }

        ServiceType serviceType;
        try {
            serviceType = ServiceType.valueOf(serviceDto.getServiceType());
        } catch (IllegalArgumentException e) {
            throw new ChikChakException("Invalid service type: " + serviceDto.getServiceType());
        }

        if (serviceCategoryRepository.findByServiceType(serviceType).isPresent()) {
            throw new ChikChakException("Service category with this service type already exists");
        }

        ServiceCategory service = userMapper.toEntity(serviceDto);
        service.setServiceType(serviceType);
        service = serviceCategoryRepository.save(service);

        log.info("New service category created: {} ({})", service.getName(), service.getServiceType());
        return userMapper.toDto(service);
    }

    public ServiceCategoryDto updateService(Long id, ServiceCategoryDto serviceDto) {
        ServiceCategory service = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new ChikChakException("Service category not found"));

        if (serviceDto.getName() != null) {
            service.setName(serviceDto.getName());
        }
        if (serviceDto.getNameHe() != null) {
            service.setNameHe(serviceDto.getNameHe());
        }
        if (serviceDto.getDescription() != null) {
            service.setDescription(serviceDto.getDescription());
        }
        if (serviceDto.getIconUrl() != null) {
            service.setIconUrl(serviceDto.getIconUrl());
        }
        if (serviceDto.getIsActive() != null) {
            service.setIsActive(serviceDto.getIsActive());
        }

        service = serviceCategoryRepository.save(service);
        log.info("Service category updated: {}", service.getName());

        return userMapper.toDto(service);
    }
}