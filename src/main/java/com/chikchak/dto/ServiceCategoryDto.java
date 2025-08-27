package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategoryDto {
    private Long id;
    private String name;
    private String nameHe;
    private String description;
    private String iconUrl;
    private Boolean isActive;
}
