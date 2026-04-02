package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTypeDto {
    private Long id;
    @Builder.Default
    private String name = "";
    @Builder.Default
    private String nameHe = "";
    private Long serviceCategoryId;
}
