package com.chikchak.mapper;

import com.chikchak.dto.JobTypeDto;
import com.chikchak.model.JobType;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobTypeMapper {

    JobTypeDto toDto(JobType jobType);

    List<JobTypeDto> toDtoList(List<JobType> jobTypes);
}
