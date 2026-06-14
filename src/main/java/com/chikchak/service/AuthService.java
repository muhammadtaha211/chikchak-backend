package com.chikchak.service;

import com.chikchak.dto.*;
import com.chikchak.exception.ChikChakException;
import com.chikchak.mapper.UserMapper;
import com.chikchak.model.*;
import com.chikchak.repository.BookingRepository;
import com.chikchak.repository.BookingStatusHistoryRepository;
import com.chikchak.repository.DeviceTokenRepository;
import com.chikchak.repository.TechnicianProfileRepository;
import com.chikchak.repository.UserRepository;
import com.chikchak.security.CustomUserDetails;
import com.chikchak.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chikchak.config.DataInitializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
            );

            Boolean isVerified = null;
            if (user.getRole() == UserRole.TECHNICIAN && user.getTechnicianProfile() != null) {
                isVerified = user.getTechnicianProfile().getIsVerified();
            }

            UserDto userDto = userMapper.toDto(user);

            log.info("User {} logged in successfully", user.getEmail());

            return AuthResponse.builder()
                .token(token)
                .isVerified(isVerified)
                .user(userDto)
                .build();

        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequest.getEmail(), e);
            throw new ChikChakException("Invalid email or password");
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ChikChakException("Email is already registered");
        }

        // Create user
        User user = User.builder()
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .fullName(registerRequest.getFullName())
            .phoneNumber(registerRequest.getPhoneNumber())
            .role(registerRequest.getRole())
            .preferredLanguage(registerRequest.getPreferredLanguage())
            .address(registerRequest.getAddress())
            .city(registerRequest.getCity())
            .latitude(registerRequest.getLatitude())
            .longitude(registerRequest.getLongitude())
            .build();

        user = userRepository.save(user);

        // Create technician profile if user is a technician
        Boolean isVerified = null;
        if (registerRequest.getRole() == UserRole.TECHNICIAN) {
            TechnicianProfile profile = createTechnicianProfile(user, registerRequest);
            isVerified = profile.getIsVerified();
        }

        // Generate token
        String token = jwtTokenProvider.generateToken(
            user.getEmail(),
            user.getId(),
            user.getRole().name()
        );

        UserDto userDto = userMapper.toDto(user);

        log.info("User {} registered successfully as {}", user.getEmail(), user.getRole());

        return AuthResponse.builder()
            .token(token)
            .isVerified(isVerified)
            .user(userDto)
            .build();
    }

    private TechnicianProfile createTechnicianProfile(User user, RegisterRequest registerRequest) {
        ServiceType serviceType = registerRequest.getServiceType();
        if (serviceType == null) {
            throw new ChikChakException("Service type is required for technicians");
        }

        BigDecimal diagnosisFee = serviceType == ServiceType.MOVERS
            ? DataInitializer.MOVERS_DIAGNOSIS_FEE
            : DataInitializer.STANDARD_DIAGNOSIS_FEE;

        if (registerRequest.getYearsExperience() == null) {
            throw new ChikChakException("Years of experience is required for technicians");
        }

        TechnicianProfile profile = TechnicianProfile.builder()
            .user(user)
            .baseDiagnosisFee(diagnosisFee)
            .bio(registerRequest.getBio())
            .yearsExperience(registerRequest.getYearsExperience())
            .isVerified(false) // Requires admin approval
            .isAvailable(false)
            .serviceType(serviceType)
            .build();

        return technicianProfileRepository.save(profile);
    }

    public UserDto getCurrentUser() {
        User user = getCurrentUserEntity();
        return userMapper.toDto(user);
    }

    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // AnonymousAuthenticationToken reports isAuthenticated() = true but its principal
        // is the String "anonymousUser" — without the instanceof check the cast below
        // throws ClassCastException (HTTP 500) on permitAll endpoints like /api/auth/me
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new ChikChakException("User not authenticated");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser();
    }

    public UserDto updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUserEntity();
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getLatitude() != null) {
            user.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            user.setLongitude(request.getLongitude());
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        
        return userMapper.toDto(user);
    }

    public UserDto updateProfileImage(UpdateProfileImageRequest request) {
        User user = getCurrentUserEntity();

        user.setProfileImageUrl(request.getProfileImageUrl());
        user = userRepository.save(user);
        log.info("Profile image updated for user: {}", user.getEmail());

        return userMapper.toDto(user);
    }

    public void changePassword(String currentPassword, String newPassword) {
        User user = getCurrentUserEntity();
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ChikChakException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getEmail());
    }

    /**
     * Permanently deletes the current user's account (soft-delete + anonymize).
     * The user row is kept so that the other party's history stays intact, but every
     * identifying field is scrubbed, the password is scrambled and the status is flipped
     * to DELETED so the account can never be authenticated again (the JWT filter already
     * rejects non-ACTIVE users).
     *
     * In-flight bookings are resolved by role: a leaving customer's active bookings are
     * deleted outright (they are the customer's own data and no payments are involved),
     * while a leaving technician's active jobs are cancelled so the customer — who stays —
     * sees the cancellation and can rebook with someone else.
     */
    public void deleteAccount(DeleteAccountRequest request) {
        User user = getCurrentUserEntity();

        // Admins manage other accounts; they must not self-delete through this endpoint
        if (user.getRole() == UserRole.ADMIN) {
            throw new ChikChakException("Admin accounts cannot be deleted through this endpoint");
        }

        // Re-confirm identity — guards against a stolen/unattended session
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ChikChakException("Password is incorrect");
        }

        // Resolve in-flight bookings before scrubbing the account
        if (user.getRole() == UserRole.CUSTOMER) {
            List<Booking> activeBookings = bookingRepository.findNonTerminalBookingsForCustomer(user.getId());
            // Cascade removes each booking's chat, messages and status history
            bookingRepository.deleteAll(activeBookings);
        } else if (user.getRole() == UserRole.TECHNICIAN && user.getTechnicianProfile() != null) {
            List<Booking> activeJobs = bookingRepository.findNonTerminalBookingsForTechnician(user.getTechnicianProfile().getId());
            for (Booking booking : activeJobs) {
                cancelBookingForLeavingTechnician(booking, user.getEmail());
            }
        }

        String originalEmail = user.getEmail();

        // Anonymize PII. The id-based placeholder email is unique and frees the
        // original address so it can be used to register a fresh account later.
        user.setEmail("deleted_" + user.getId() + "@deleted.chikchak");
        user.setFullName("Deleted User");
        user.setPhoneNumber(null);
        user.setProfileImageUrl(null);
        user.setAddress(null);
        user.setCity(null);
        user.setLatitude(null);
        user.setLongitude(null);
        // Scramble the password so the account can never be authenticated again
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.DELETED);

        // Take a deleted technician out of search/matching
        if (user.getRole() == UserRole.TECHNICIAN && user.getTechnicianProfile() != null) {
            user.getTechnicianProfile().setIsAvailable(false);
        }

        userRepository.save(user);

        // Stop all push notifications to this user's devices
        deviceTokenRepository.deleteByUserId(user.getId());

        log.info("Account permanently deleted (anonymized) for user id {} (was {})", user.getId(), originalEmail);
    }

    // Cancels a job left behind by a technician who is deleting their account: keeps the
    // booking (it belongs to the customer who stays) but marks it CANCELLED, records the
    // transition, and notifies the customer so they can rebook.
    private void cancelBookingForLeavingTechnician(Booking booking, String technicianEmail) {
        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledDatetime(LocalDateTime.now());
        bookingRepository.save(booking);

        statusHistoryRepository.save(BookingStatusHistory.builder()
                .booking(booking)
                .oldStatus(previousStatus)
                .newStatus(BookingStatus.CANCELLED)
                .changedBy(technicianEmail)
                .reason("Technician deleted their account")
                .build());

        notificationService.notifyBookingCancellation(booking);
    }
}