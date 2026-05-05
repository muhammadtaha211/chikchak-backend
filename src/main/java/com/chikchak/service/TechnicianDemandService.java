package com.chikchak.service;

import com.chikchak.dto.CreateDemandRequestDto;
import com.chikchak.dto.TechnicianDemandRequestDto;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.TechnicianDemandRequestMapper;
import com.chikchak.model.DemandStatus;
import com.chikchak.model.TechnicianDemandRequest;
import com.chikchak.model.User;
import com.chikchak.model.UserRole;
import com.chikchak.repository.TechnicianDemandRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TechnicianDemandService {

    private final TechnicianDemandRequestRepository repository;
    private final AuthService authService;
    private final TechnicianDemandRequestMapper mapper;

    public TechnicianDemandRequestDto createDemandRequest(CreateDemandRequestDto request) {
        User currentUser = authService.getCurrentUserEntity();

        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new ChikChakException("Only customers can register interest in technician availability");
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        Optional<TechnicianDemandRequest> existing = repository
                .findByUserIdAndStatus(currentUser.getId(), DemandStatus.PENDING)
                .stream()
                .filter(r -> r.getServiceType() == request.getServiceType())
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(cutoff))
                .findFirst();

        if (existing.isPresent()) {
            log.info("Returning existing demand request for user {}: serviceType={}",
                    currentUser.getEmail(), request.getServiceType());
            return mapper.toDto(existing.get());
        }

        TechnicianDemandRequest entity = TechnicianDemandRequest.builder()
                .user(currentUser)
                .serviceType(request.getServiceType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .status(DemandStatus.PENDING)
                .build();

        entity = repository.save(entity);

        log.info("Demand request created for user {}: serviceType={}, location=({}, {})",
                currentUser.getEmail(), entity.getServiceType(), entity.getLatitude(), entity.getLongitude());

        return mapper.toDto(entity);
    }
}
