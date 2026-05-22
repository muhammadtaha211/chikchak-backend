package com.chikchak.dto;

import com.chikchak.model.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    @Builder.Default
    private String nameAr = "";
    private ServiceType serviceType;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
