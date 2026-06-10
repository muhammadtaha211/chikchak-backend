package com.chikchak.controller;

import com.chikchak.dto.ApiResponse;
import com.chikchak.dto.RegisterDeviceTokenRequest;
import com.chikchak.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "General user APIs")
public class UserController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/device-token")
    @Operation(summary = "Register device token", description = "Register an FCM device token for push notifications")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        deviceTokenService.registerToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Device token registered", null));
    }
}
