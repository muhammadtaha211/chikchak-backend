package com.chikchak.mapper;
// Mappers using MapStruct

import com.chikchak.dto.UserDto;
import com.chikchak.dto.TechnicianProfileDto;
import com.chikchak.dto.ServiceCategoryDto;
import com.chikchak.model.User;
import com.chikchak.model.TechnicianProfile;
import com.chikchak.model.ServiceCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDto toDto(User user);

    User toEntity(UserDto userDto);

    @Mapping(target = "distanceFromCustomer", ignore = true)
    @Mapping(source = "user", target = "user")
    TechnicianProfileDto toDto(TechnicianProfile technicianProfile);

    TechnicianProfile toEntity(TechnicianProfileDto technicianProfileDto);

    ServiceCategoryDto toDto(ServiceCategory serviceCategory);

    @Mapping(target = "serviceType", ignore = true)
    ServiceCategory toEntity(ServiceCategoryDto serviceCategoryDto);
}
