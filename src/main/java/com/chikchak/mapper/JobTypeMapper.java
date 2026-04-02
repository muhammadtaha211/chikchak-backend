package com.chikchak.mapper;

import com.chikchak.dto.JobTypeDto;
import com.chikchak.model.JobType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobTypeMapper {

    @Mapping(source = "serviceCategory.id", target = "serviceCategoryId")
    JobTypeDto toDto(JobType jobType);

    List<JobTypeDto> toDtoList(List<JobType> jobTypes);
}
