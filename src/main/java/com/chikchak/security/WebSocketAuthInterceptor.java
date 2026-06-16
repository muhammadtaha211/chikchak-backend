package com.chikchak.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP connections. The /ws endpoint is permitAll at the HTTP layer, so
 * without this every WebSocket client could connect and subscribe unauthenticated. Here we
 * require a valid Bearer token on the CONNECT frame and bind the authenticated user to the
 * session (used for user-destination push).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearer = accessor.getFirstNativeHeader("Authorization");
            String token = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;

            if (token == null || !tokenProvider.validateToken(token)) {
                throw new MessagingException("Unauthorized WebSocket connection");
            }

            String email = tokenProvider.getEmailFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (userDetails == null || !userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                throw new MessagingException("User is not allowed to connect");
            }

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
            log.debug("WebSocket authenticated for {}", email);
        }

        return message;
    }
}
