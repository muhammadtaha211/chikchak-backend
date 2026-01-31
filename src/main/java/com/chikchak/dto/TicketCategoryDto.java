package com.chikchak.dto;

import com.chikchak.model.*;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCategoryDto {
    private TicketCategory value;
    private String displayName;

    public static List<TicketCategoryDto> getAllCategories() {
        return java.util.Arrays.stream(TicketCategory.values())
                .map(cat -> TicketCategoryDto.builder()
                        .value(cat)
                        .displayName(cat.getDisplayName())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}