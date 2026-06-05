package com.chikchak.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPhotoRequest {
    @NotEmpty(message = "At least one photo URL is required")
    private List<String> photoUrls;
}
