package com.chikchak.mapper;

import com.chikchak.dto.*;
import com.chikchak.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = UserMapper.class)
public interface BookingMapper {

    @Mapping(target = "photos", source = "photos", qualifiedByName = "stringToList")
    @Mapping(target = "unreadMessagesCount", ignore = true)
    BookingDto toDto(Booking booking);

    @Mapping(target = "photos", source = "photos", qualifiedByName = "listToString")
    Booking toEntity(BookingDto bookingDto);

    ChatMessageDto toDto(ChatMessage chatMessage);

    ChatMessage toEntity(ChatMessageDto chatMessageDto);

    PaymentTransactionDto toDto(PaymentTransaction paymentTransaction);

    PaymentTransaction toEntity(PaymentTransactionDto paymentTransactionDto);

    @org.mapstruct.Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }

    @org.mapstruct.Named("listToString")
    default String listToString(List<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return String.join(",", value);
    }


    // Chat mapping methods
    ChatDto toDto(Chat chat);
    Chat toEntity(ChatDto chatDto);


}
