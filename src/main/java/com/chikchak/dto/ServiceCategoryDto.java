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

    @Builder.Default
    private String name = "";

    @Builder.Default
    private String nameHe = "";

    @Builder.Default
    private String description = "";

    @Builder.Default
    private String iconUrl = "";

    @Builder.Default
    private Boolean isActive = true;
}