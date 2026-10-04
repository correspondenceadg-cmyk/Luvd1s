package ws.tilda.sentryprotocol.Luvd1s.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiter {

    private final int maxRequests;
    private final long windowSeconds;

    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    public RateLimiter(@Value("${luvd1s.ai.rate-limit.max:20}") int maxRequests,
                       @Value("${luvd1s.ai.rate-limit.window-seconds:3600}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) return true;

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(windowSeconds);

        Deque<Instant> bucket = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
                bucket.pollFirst();
            }
            if (bucket.size() >= maxRequests) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }

    public int remaining(String key) {
        if (key == null || key.isBlank()) return maxRequests;
        Deque<Instant> bucket = buckets.get(key);
        if (bucket == null) return maxRequests;

        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
                bucket.pollFirst();
            }
            return Math.max(0, maxRequests - bucket.size());
        }
    }

    public int maxRequests() {
        return maxRequests;
    }

    public long windowSeconds() {
        return windowSeconds;
    }
}