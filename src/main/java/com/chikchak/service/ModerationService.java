package com.chikchak.service;

import com.chikchak.dto.CreateReportRequest;
import com.chikchak.dto.UserDto;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.UserMapper;
import com.chikchak.model.ContentReport;
import com.chikchak.model.ReportStatus;
import com.chikchak.model.User;
import com.chikchak.model.UserBlock;
import com.chikchak.repository.ContentReportRepository;
import com.chikchak.repository.UserBlockRepository;
import com.chikchak.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// User-facing UGC moderation: blocking other users and reporting users/content.
// Admin review of reports lives in AdminService.
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ModerationService {

    private final UserBlockRepository userBlockRepository;
    private final ContentReportRepository contentReportRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserMapper userMapper;
    private final BookingService bookingService;

    public void blockUser(Long targetUserId) {
        User me = authService.getCurrentUserEntity();
        if (me.getId().equals(targetUserId)) {
            throw new ChikChakException("You cannot block yourself");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ChikChakException("User not found"));

        // Idempotent: silently succeed if already blocked.
        if (userBlockRepository.existsByBlocker_IdAndBlocked_Id(me.getId(), target.getId())) {
            return;
        }

        userBlockRepository.save(UserBlock.builder()
                .blocker(me)
                .blocked(target)
                .build());

        // Sever any in-progress relationship: auto-cancel active booking(s) between the two.
        bookingService.cancelActiveBookingsDueToBlock(me.getId(), target.getId());

        log.info("User {} blocked user {}", me.getId(), target.getId());
    }

    public void unblockUser(Long targetUserId) {
        User me = authService.getCurrentUserEntity();
        userBlockRepository.deleteByBlocker_IdAndBlocked_Id(me.getId(), targetUserId);
        log.info("User {} unblocked user {}", me.getId(), targetUserId);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getBlockedUsers() {
        User me = authService.getCurrentUserEntity();
        return userBlockRepository.findByBlocker_Id(me.getId()).stream()
                .map(block -> userMapper.toDto(block.getBlocked()))
                .collect(Collectors.toList());
    }

    public void createReport(CreateReportRequest request) {
        User me = authService.getCurrentUserEntity();
        if (me.getId().equals(request.getReportedUserId())) {
            throw new ChikChakException("You cannot report yourself");
        }
        User reported = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new ChikChakException("Reported user not found"));

        contentReportRepository.save(ContentReport.builder()
                .reporter(me)
                .reportedUser(reported)
                .chatMessageId(request.getChatMessageId())
                .ratingId(request.getRatingId())
                .reason(request.getReason())
                .status(ReportStatus.OPEN)
                .build());

        log.info("User {} reported user {} (message={}, rating={})",
                me.getId(), reported.getId(), request.getChatMessageId(), request.getRatingId());
    }
}
