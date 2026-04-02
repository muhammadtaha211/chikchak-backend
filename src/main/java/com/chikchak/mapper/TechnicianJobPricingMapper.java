package com.chikchak.mapper;

import com.chikchak.dto.TechnicianJobPricingDto;
import com.chikchak.model.TechnicianJobPricing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TechnicianJobPricingMapper {

    @Mapping(source = "jobType.id", target = "jobTypeId")
    @Mapping(source = "jobType.name", target = "jobTypeName")
    @Mapping(source = "jobType.nameHe", target = "jobTypeNameHe")
    TechnicianJobPricingDto toDto(TechnicianJobPricing pricing);

    List<TechnicianJobPricingDto> toDtoList(List<TechnicianJobPricing> pricings);
}
