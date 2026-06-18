package com.chikchak.service;

import com.chikchak.dto.AccountDeletionRequestDto;
import com.chikchak.model.AccountDeletionRequest;
import com.chikchak.model.DeletionRequestStatus;
import com.chikchak.repository.AccountDeletionRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Captures public, no-login account-deletion requests. The stored row is the source of
// truth and is reviewed/fulfilled by an admin (see AdminService.processDeletionRequest);
// the operator email is only a best-effort heads-up.
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataDeletionService {

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final JavaMailSender mailSender;

    // Where to send the heads-up. Defaults to the operator's address (see application.properties).
    @Value("${chikchak.deletion.notify-email:}")
    private String notifyEmail;

    // Sending requires real SMTP credentials; when unset (e.g. local dev) we skip the email.
    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void submitDeletionRequest(AccountDeletionRequestDto dto) {
        // Honeypot: silently accept and drop bot submissions so they don't learn it's a trap.
        if (dto.getWebsite() != null && !dto.getWebsite().isBlank()) {
            log.warn("Account-deletion honeypot triggered; dropping spam submission");
            return;
        }

        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .email(dto.getEmail().trim().toLowerCase())
                .fullName(trimToNull(dto.getFullName()))
                .phoneNumber(trimToNull(dto.getPhoneNumber()))
                .reason(trimToNull(dto.getReason()))
                .status(DeletionRequestStatus.PENDING)
                .build();
        deletionRequestRepository.save(request);

        log.info("Account-deletion request #{} recorded for {}", request.getId(), request.getEmail());

        notifyOperator(request);
    }

    // Best-effort notification. Never throws — the persisted request is the durable record,
    // and pending requests are always reviewable via /api/admin/deletion-requests.
    private void notifyOperator(AccountDeletionRequest request) {
        if (notifyEmail == null || notifyEmail.isBlank() || mailFrom == null || mailFrom.isBlank()) {
            log.debug("Deletion-request notify email skipped (notify-email or SMTP sender not configured)");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(notifyEmail);
            message.setSubject("New account deletion request #" + request.getId());
            message.setText(buildBody(request));
            mailSender.send(message);
            log.info("Operator notified of deletion request #{}", request.getId());
        } catch (Exception e) {
            log.error("Failed to email operator about deletion request #{}: {}", request.getId(), e.getMessage());
        }
    }

    private String buildBody(AccountDeletionRequest request) {
        return "A new account & data deletion request was submitted.\n\n"
                + "Request #: " + request.getId() + "\n"
                + "Email:     " + request.getEmail() + "\n"
                + "Name:      " + valueOrDash(request.getFullName()) + "\n"
                + "Phone:     " + valueOrDash(request.getPhoneNumber()) + "\n"
                + "Reason:    " + valueOrDash(request.getReason()) + "\n\n"
                + "Review and process it from the admin panel "
                + "(GET /api/admin/deletion-requests, then POST /api/admin/deletion-requests/"
                + request.getId() + "/process).";
    }

    private String valueOrDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
