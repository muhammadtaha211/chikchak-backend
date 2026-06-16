package com.chikchak.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    // Per-user chat limit (existing behaviour — defence-in-depth alongside the per-IP write limit)
    private static final int CHAT_CAPACITY = 10;
    private static final Duration CHAT_REFILL_PERIOD = Duration.ofMinutes(1);

    // Per-IP limits for the global RateLimitFilter (env-overridable)
    @Value("${ratelimit.auth.capacity:5}")
    private int authCapacity;

    @Value("${ratelimit.auth.window-minutes:15}")
    private int authWindowMinutes;

    @Value("${ratelimit.write.capacity:60}")
    private int writeCapacity;

    @Value("${ratelimit.write.window-minutes:1}")
    private int writeWindowMinutes;

    // Buckets are kept in size-bounded caches with TTL eviction so per-user/per-IP
    // keys cannot grow the heap without bound.
    private final Cache<Long, Bucket> chatBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> authBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> writeBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    /** Per-user chat message limit (10 / minute). */
    public boolean tryConsume(Long userId) {
        Bucket bucket = chatBuckets.get(userId, id -> greedyBucket(CHAT_CAPACITY, CHAT_REFILL_PERIOD));
        return bucket.tryConsume(1);
    }

    /** Per-IP limit for authentication routes (default 5 / 15 min, strict fixed-interval refill). */
    public boolean tryConsumeAuth(String ip) {
        Bucket bucket = authBuckets.get(ip,
                k -> intervalBucket(authCapacity, Duration.ofMinutes(authWindowMinutes)));
        return bucket.tryConsume(1);
    }

    /** Per-IP limit for state-changing requests (default 60 / minute, smooth refill). */
    public boolean tryConsumeWrite(String ip) {
        Bucket bucket = writeBuckets.get(ip,
                k -> greedyBucket(writeCapacity, Duration.ofMinutes(writeWindowMinutes)));
        return bucket.tryConsume(1);
    }

    public int getAuthWindowMinutes() {
        return authWindowMinutes;
    }

    public int getWriteWindowMinutes() {
        return writeWindowMinutes;
    }

    // Refills the whole capacity in one chunk at the end of each window — strict "N per window".
    private Bucket intervalBucket(int capacity, Duration window) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, window)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    // Refills continuously over the window — smoother, used for high-volume write/chat limits.
    private Bucket greedyBucket(int capacity, Duration window) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, window)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
