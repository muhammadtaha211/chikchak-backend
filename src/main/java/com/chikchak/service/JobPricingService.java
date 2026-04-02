package com.chikchak.service;

import com.chikchak.dto.JobTypeDto;
import com.chikchak.dto.SaveTechnicianJobPricingRequest;
import com.chikchak.dto.TechnicianJobPricingDto;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.JobTypeMapper;
import com.chikchak.mapper.TechnicianJobPricingMapper;
import com.chikchak.model.JobType;
import com.chikchak.model.TechnicianJobPricing;
import com.chikchak.model.TechnicianProfile;
import com.chikchak.model.User;
import com.chikchak.repository.JobTypeRepository;
import com.chikchak.repository.TechnicianJobPricingRepository;
import com.chikchak.repository.TechnicianProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobPricingService {

    private final JobTypeRepository jobTypeRepository;
    private final TechnicianJobPricingRepository technicianJobPricingRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final AuthService authService;
    private final JobTypeMapper jobTypeMapper;
    private final TechnicianJobPricingMapper technicianJobPricingMapper;

    @Transactional(readOnly = true)
    public List<JobTypeDto> getJobTypesByCategory(Long serviceCategoryId) {
        List<JobType> jobTypes = jobTypeRepository.findByServiceCategoryIdOrderByName(serviceCategoryId);
        return jobTypeMapper.toDtoList(jobTypes);
    }

    @Transactional(readOnly = true)
    public List<TechnicianJobPricingDto> getTechnicianPricing(Long technicianId) {
        // Verify technician exists
        technicianProfileRepository.findById(technicianId)
                .orElseThrow(() -> new ChikChakException("Technician not found"));

        List<TechnicianJobPricing> pricings = technicianJobPricingRepository
                .findByTechnicianIdOrderByJobTypeName(technicianId);
        return technicianJobPricingMapper.toDtoList(pricings);
    }

    public List<TechnicianJobPricingDto> saveTechnicianPricing(SaveTechnicianJobPricingRequest request) {
        User currentUser = authService.getCurrentUserEntity();
        TechnicianProfile technician = technicianProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ChikChakException("Technician profile not found"));

        // Delete existing pricing
        technicianJobPricingRepository.deleteByTechnicianId(technician.getId());

        // Save new pricing
        List<TechnicianJobPricing> newPricings = new ArrayList<>();
        for (SaveTechnicianJobPricingRequest.JobPricingItem item : request.getItems()) {
            JobType jobType = jobTypeRepository.findById(item.getJobTypeId())
                    .orElseThrow(() -> new ChikChakException("Job type not found: " + item.getJobTypeId()));

            if (item.getMinPrice().compareTo(item.getMaxPrice()) > 0) {
                throw new ChikChakException("Min price cannot be greater than max price for job: " + jobType.getName());
            }

            TechnicianJobPricing pricing = TechnicianJobPricing.builder()
                    .technician(technician)
                    .jobType(jobType)
                    .minPrice(item.getMinPrice())
                    .maxPrice(item.getMaxPrice())
                    .build();
            newPricings.add(pricing);
        }

        List<TechnicianJobPricing> saved = technicianJobPricingRepository.saveAll(newPricings);
        log.info("Saved {} job pricings for technician {}", saved.size(), technician.getId());
        return technicianJobPricingMapper.toDtoList(saved);
    }
}
