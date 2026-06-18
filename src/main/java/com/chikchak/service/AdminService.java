package com.chikchak.service;

import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.AdminActionLogMapper;
import com.chikchak.mapper.BookingMapper;
import com.chikchak.mapper.UserMapper;
import com.chikchak.model.*;
import com.chikchak.repository.AccountDeletionRequestRepository;
import com.chikchak.repository.AdminActionLogRepository;
import com.chikchak.repository.BookingRepository;
import com.chikchak.repository.TechnicianProfileRepository;
import com.chikchak.repository.UserRepository;
import com.chikchak.security.UserAuthCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private static final String TARGET_TECHNICIAN = "TECHNICIAN";
    private static final String TARGET_USER = "USER";
    private static final String TARGET_BOOKING = "BOOKING";

    private final TechnicianProfileRepository technicianProfileRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final AuthService authService;
    private final BookingService bookingService;
    private final UserMapper userMapper;
    private final BookingMapper bookingMapper;
    private final AdminActionLogMapper adminActionLogMapper;
    private final UserAuthCache userAuthCache;

    // ─── Technician approval ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TechnicianProfileDto> getPendingTechnicians(Pageable pageable) {
        return technicianProfileRepository.findPendingApproval(pageable).getContent().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public TechnicianProfileDto approveTechnician(Long technicianId) {
        TechnicianProfile technician = findTechnicianById(technicianId);

        if (Boolean.TRUE.equals(technician.getIsVerified())) {
            throw new ChikChakException("Technician is already verified");
        }

        technician.setIsVerified(true);
        technician = technicianProfileRepository.save(technician);

        logAction(AdminActionType.TECHNICIAN_APPROVED, TARGET_TECHNICIAN, technicianId, null);

        log.info("Technician {} ({}) approved by admin",
                technicianId, technician.getUser().getEmail());

        return userMapper.toDto(technician);
    }

    public TechnicianProfileDto rejectTechnician(Long technicianId, String reason) {
        TechnicianProfile technician = findTechnicianById(technicianId);

        if (Boolean.TRUE.equals(technician.getIsVerified())) {
            throw new ChikChakException("Cannot reject an already verified technician");
        }

        // Rejection does not change the technician — isVerified stays false; only logged
        logAction(AdminActionType.TECHNICIAN_REJECTED, TARGET_TECHNICIAN, technicianId, reason);

        log.info("Technician {} ({}) rejected by admin. Reason: {}",
                technicianId, technician.getUser().getEmail(), reason);

        return userMapper.toDto(technician);
    }

    @Transactional(readOnly = true)
    public List<TechnicianProfileDto> getAllTechnicians(Boolean verified, String search, Pageable pageable) {
        return technicianProfileRepository.searchTechniciansForAdmin(verified, normalize(search), pageable)
                .getContent().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TechnicianProfileDto getTechnicianDetail(Long technicianId) {
        return userMapper.toDto(findTechnicianById(technicianId));
    }

    // ─── User management ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserDto> getUsers(UserRole role, UserStatus status, String search, Pageable pageable) {
        return userRepository.searchUsersForAdmin(role, status, normalize(search), pageable)
                .getContent().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserDetail(Long userId) {
        return userMapper.toDto(findUserById(userId));
    }

    public UserDto suspendUser(Long userId, String reason) {
        User user = findUserById(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new ChikChakException("Cannot suspend an admin user");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ChikChakException("User is already suspended");
        }

        user.setStatus(UserStatus.SUSPENDED);
        user = userRepository.save(user);
        // Drop any cached auth entry so the suspension takes effect on the next request
        userAuthCache.evict(user.getEmail());

        logAction(AdminActionType.USER_SUSPENDED, TARGET_USER, userId, reason);

        log.info("User {} ({}) suspended by admin. Reason: {}", userId, user.getEmail(), reason);

        return userMapper.toDto(user);
    }

    public UserDto reactivateUser(Long userId) {
        User user = findUserById(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ChikChakException("User is already active");
        }

        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        // Drop the cached (suspended) auth entry so the user can authenticate again immediately
        userAuthCache.evict(user.getEmail());

        logAction(AdminActionType.USER_REACTIVATED, TARGET_USER, userId, null);

        log.info("User {} ({}) reactivated by admin", userId, user.getEmail());

        return userMapper.toDto(user);
    }

    // ─── Booking management ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BookingDto> getBookings(BookingStatus status, String search, Pageable pageable) {
        return bookingRepository.searchBookingsForAdmin(status, normalize(search), pageable)
                .getContent().stream()
                .map(bookingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingDetail(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));
        return bookingMapper.toDto(booking);
    }

    public BookingDto cancelBooking(Long bookingId, String reason) {
        BookingDto result = bookingService.adminUpdateBookingStatus(bookingId, BookingStatus.CANCELLED, reason);

        logAction(AdminActionType.BOOKING_CANCELLED, TARGET_BOOKING, bookingId, reason);

        return result;
    }

    public BookingDto forceBookingStatus(Long bookingId, BookingStatus newStatus, String reason) {
        if (newStatus == null) {
            throw new ChikChakException("Status is required");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ChikChakException("Booking not found"));
        BookingStatus oldStatus = booking.getStatus();

        BookingDto result = bookingService.adminUpdateBookingStatus(bookingId, newStatus, reason);

        String details = "Status forced from " + oldStatus + " to " + newStatus
                + (reason != null && !reason.trim().isEmpty() ? ". Reason: " + reason : "");
        logAction(AdminActionType.BOOKING_STATUS_OVERRIDE, TARGET_BOOKING, bookingId, details);

        return result;
    }

    // ─── Account deletion requests ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeletionRequestDto> getDeletionRequests(DeletionRequestStatus status, Pageable pageable) {
        Page<AccountDeletionRequest> requests = (status == null)
                ? deletionRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                : deletionRequestRepository.findByStatusOrderByCreatedAtAsc(status, pageable);
        return requests.getContent().stream()
                .map(this::toDeletionDto)
                .collect(Collectors.toList());
    }

    // Fulfills a request: anonymizes the matching account (reusing AuthService) and marks it PROCESSED.
    public DeletionRequestDto processDeletionRequest(Long requestId) {
        AccountDeletionRequest request = findDeletionRequestById(requestId);
        requirePending(request);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ChikChakException("No account found for " + request.getEmail()));

        authService.performAccountDeletion(user);

        markResolved(request, DeletionRequestStatus.PROCESSED);
        logAction(AdminActionType.USER_DELETED, TARGET_USER, user.getId(),
                "Fulfilled deletion request #" + requestId + " for " + request.getEmail());

        log.info("Deletion request #{} processed: user id {} anonymized", requestId, user.getId());

        return toDeletionDto(request);
    }

    // Dismisses a request (e.g. spam or no matching account) without touching any account.
    public DeletionRequestDto rejectDeletionRequest(Long requestId, String reason) {
        AccountDeletionRequest request = findDeletionRequestById(requestId);
        requirePending(request);

        markResolved(request, DeletionRequestStatus.REJECTED);
        logAction(AdminActionType.DELETION_REQUEST_REJECTED, TARGET_USER, requestId, reason);

        log.info("Deletion request #{} rejected. Reason: {}", requestId, reason);

        return toDeletionDto(request);
    }

    // ─── Audit log ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminActionLogDto> getActionLogs(Pageable pageable) {
        return adminActionLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .getContent().stream()
                .map(adminActionLogMapper::toDto)
                .collect(Collectors.toList());
    }

    private void logAction(AdminActionType actionType, String targetType, Long targetId, String details) {
        User admin = authService.getCurrentUserEntity();

        adminActionLogRepository.save(AdminActionLog.builder()
                .adminUserId(admin.getId())
                .adminEmail(admin.getEmail())
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build());
    }

    private TechnicianProfile findTechnicianById(Long technicianId) {
        return technicianProfileRepository.findById(technicianId)
                .orElseThrow(() -> new ChikChakException("Technician not found"));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ChikChakException("User not found"));
    }

    private AccountDeletionRequest findDeletionRequestById(Long requestId) {
        return deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ChikChakException("Deletion request not found"));
    }

    private void requirePending(AccountDeletionRequest request) {
        if (request.getStatus() != DeletionRequestStatus.PENDING) {
            throw new ChikChakException("Deletion request has already been "
                    + request.getStatus().name().toLowerCase());
        }
    }

    private void markResolved(AccountDeletionRequest request, DeletionRequestStatus status) {
        request.setStatus(status);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(authService.getCurrentUserEntity().getEmail());
        deletionRequestRepository.save(request);
    }

    private DeletionRequestDto toDeletionDto(AccountDeletionRequest request) {
        return DeletionRequestDto.builder()
                .id(request.getId())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .reason(request.getReason())
                .status(request.getStatus())
                .requestedAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .processedBy(request.getProcessedBy())
                .build();
    }

    // Queries require a non-null search term; empty string matches all rows
    private String normalize(String search) {
        return search != null ? search.trim() : "";
    }
}
