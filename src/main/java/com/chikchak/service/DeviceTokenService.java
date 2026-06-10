package com.chikchak.service;

import com.chikchak.model.DeviceToken;
import com.chikchak.model.User;
import com.chikchak.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final AuthService authService;

    // Upsert: same token re-registered (e.g. re-login on same device) must not duplicate.
    // If the token belongs to another user (device changed accounts), reassign it.
    public void registerToken(String token) {
        User currentUser = authService.getCurrentUserEntity();

        DeviceToken deviceToken = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());

        deviceToken.setUser(currentUser);
        deviceTokenRepository.save(deviceToken);

        log.info("Registered device token for user {}", currentUser.getEmail());
    }
}
