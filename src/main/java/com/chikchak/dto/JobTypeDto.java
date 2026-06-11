package com.chikchak.dto;

import com.chikchak.model.ServiceType;
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
    @Builder.Default
    private String nameAr = "";
    private ServiceType serviceType;
    // Integer (whole shekels) — FlutterFlow expects int price estimates, entity stays BigDecimal
    private Integer minPrice;
    private Integer maxPrice;
}
