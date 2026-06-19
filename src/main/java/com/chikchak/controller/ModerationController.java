package com.chikchak.controller;

import com.chikchak.dto.ApiResponse;
import com.chikchak.dto.CreateReportRequest;
import com.chikchak.dto.UserDto;
import com.chikchak.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// User-facing UGC safety controls (block / report). Any authenticated user.
@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "User safety controls: block and report users / content")
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/block/{userId}")
    @Operation(summary = "Block a user", description = "Block another user; neither party can message the other afterwards")
    public ResponseEntity<ApiResponse<String>> blockUser(@PathVariable Long userId) {
        moderationService.blockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User blocked", null));
    }

    @DeleteMapping("/block/{userId}")
    @Operation(summary = "Unblock a user", description = "Remove a block you previously placed on a user")
    public ResponseEntity<ApiResponse<String>> unblockUser(@PathVariable Long userId) {
        moderationService.unblockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked", null));
    }

    @GetMapping("/blocked")
    @Operation(summary = "List blocked users", description = "List users the current user has blocked")
    public ResponseEntity<ApiResponse<List<UserDto>>> getBlockedUsers() {
        return ResponseEntity.ok(ApiResponse.success(moderationService.getBlockedUsers()));
    }

    @PostMapping("/report")
    @Operation(summary = "Report a user or content", description = "Report a user and optionally a specific chat message or rating; sent to admins for review")
    public ResponseEntity<ApiResponse<String>> report(@Valid @RequestBody CreateReportRequest request) {
        moderationService.createReport(request);
        return ResponseEntity.ok(ApiResponse.success("Report submitted", null));
    }
}
