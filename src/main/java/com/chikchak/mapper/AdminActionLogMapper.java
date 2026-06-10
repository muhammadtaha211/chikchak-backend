package com.chikchak.mapper;

import com.chikchak.dto.AdminActionLogDto;
import com.chikchak.model.AdminActionLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminActionLogMapper {

    AdminActionLogDto toDto(AdminActionLog log);
}
