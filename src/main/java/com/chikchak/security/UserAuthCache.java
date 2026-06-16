package com.chikchak.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Function;

/**
 * Short-TTL cache of authenticated users, keyed by email, used by {@link JwtAuthenticationFilter}
 * so a heavily-polling client does not trigger a {@code users} table lookup on every request.
 *
 * <p>Login / credential authentication does NOT use this cache (it calls the UserDetailsService
 * directly), so passwords are always checked against fresh state. The cached {@link UserDetails}
 * wraps a now-detached User, so callers must treat it as read-only: write paths that mutate the
 * current user re-load a managed entity. Account-status changes (suspend / reactivate / delete)
 * and profile updates {@link #evict(String)} the entry, so a change takes effect within at most
 * the TTL — and immediately when eviction runs.
 */
@Component
public class UserAuthCache {

    private final Cache<String, UserDetails> cache;

    public UserAuthCache(@Value("${auth.cache.ttl-seconds:30}") long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(50_000)
                .build();
    }

    /**
     * Returns the cached UserDetails for the email, loading and caching it via {@code loader} on a
     * miss. The loader must throw (not return {@code null}) for an unknown user; nulls are not cached.
     */
    public UserDetails get(String email, Function<String, UserDetails> loader) {
        return cache.get(email, loader);
    }

    public void evict(String email) {
        if (email != null) {
            cache.invalidate(email);
        }
    }
}
