package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianSearchResponse {
    private List<TechnicianProfileDto> technicians;
    private int totalElements;
    private int totalPages;
    private int currentPage;
}
