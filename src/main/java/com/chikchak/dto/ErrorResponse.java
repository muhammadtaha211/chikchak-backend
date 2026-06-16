package com.chikchak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String error;
    private String message;
    private String timestamp;
    private int status;
    private String path;
    // Populated only for validation failures: field name -> message. Null otherwise.
    private Map<String, String> fieldErrors;
}
