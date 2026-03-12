package com.chikchak.mapper;

import com.chikchak.dto.RatingDto;
import com.chikchak.model.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RatingMapper {

    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "booking.bookingNumber", target = "bookingNumber")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.fullName", target = "customerName")
    @Mapping(source = "technician.id", target = "technicianId")
    RatingDto toDto(Rating rating);
}
