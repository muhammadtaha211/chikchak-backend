package com.chikchak.security;

import com.chikchak.dto.ErrorResponse;
import com.chikchak.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Per-IP rate limiting + body-size guard, applied before authentication so failed
 * logins are counted. Auth routes get a strict limit; other writes (POST/PUT/DELETE/PATCH)
 * get a generous per-IP limit; reads (GET/HEAD/OPTIONS) pass through untouched so the
 * app's 2-second polling is never throttled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    private static final Set<String> AUTH_PATHS = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/reset-password");

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Value("${request.max-json-bytes:1048576}")
    private long maxJsonBytes;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = resolveClientIp(request);

        // Strict limit on authentication routes (brute-force / credential-stuffing protection)
        if (AUTH_PATHS.contains(path)) {
            if (!rateLimitService.tryConsumeAuth(ip)) {
                reject(request, response, HttpStatus.TOO_MANY_REQUESTS,
                        "Too many attempts. Please try again later.",
                        (long) rateLimitService.getAuthWindowMinutes() * 60);
                return;
            }
        } else if (WRITE_METHODS.contains(method)) {
            // Reject oversized bodies before consuming a token or reading the stream
            if (isOversizedJson(request)) {
                reject(request, response, HttpStatus.PAYLOAD_TOO_LARGE,
                        "Request payload too large.", null);
                return;
            }
            if (!rateLimitService.tryConsumeWrite(ip)) {
                reject(request, response, HttpStatus.TOO_MANY_REQUESTS,
                        "Too many requests. Please slow down.",
                        (long) rateLimitService.getWriteWindowMinutes() * 60);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Multipart uploads have their own size limit (spring.servlet.multipart.*); everything
    // else (JSON / form) is capped here by Content-Length when the client declares one.
    private boolean isOversizedJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            return false;
        }
        return request.getContentLengthLong() > maxJsonBytes;
    }

    // Honour the proxy chain (ngrok / reverse proxy) — the originating client is the first
    // hop of X-Forwarded-For; otherwise every user would share the proxy's address.
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > -1 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
                        HttpStatus status, String message, Long retryAfterSeconds) throws IOException {
        log.warn("Request rejected ({}): {} {} from {}", status.value(), request.getMethod(),
                request.getRequestURI(), resolveClientIp(request));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (retryAfterSeconds != null) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }

        ErrorResponse body = ErrorResponse.builder()
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .status(status.value())
                .path(request.getRequestURI())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
