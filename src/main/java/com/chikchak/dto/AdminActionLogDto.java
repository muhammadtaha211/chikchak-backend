package com.chikchak.dto;

import com.chikchak.model.AdminActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActionLogDto {
    private Long id;
    private Long adminUserId;

    @Builder.Default
    private String adminEmail = "";

    private AdminActionType actionType;

    @Builder.Default
    private String targetType = "";

    private Long targetId;

    @Builder.Default
    private String details = "";

    private LocalDateTime createdAt;
}
