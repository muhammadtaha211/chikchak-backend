package com.chikchak.mapper;

import com.chikchak.dto.TechnicianDemandRequestDto;
import com.chikchak.model.TechnicianDemandRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TechnicianDemandRequestMapper {

    @Mapping(source = "user.id", target = "userId")
    TechnicianDemandRequestDto toDto(TechnicianDemandRequest entity);
}
