package ws.tilda.sentryprotocol.Luvd1s.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private final int max;
    private final long windowSeconds;
    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public RateLimiter(@Value("${luvd1s.ai.rate-limit.max:20}") int max,
                       @Value("${luvd1s.ai.rate-limit.window-seconds:3600}") long windowSeconds) {
        this.max = max;
        this.windowSeconds = windowSeconds;
    }

    public synchronized boolean tryAcquire(String key) {
        if (key == null) return false;
        long now = System.currentTimeMillis();
        Deque<Long> q = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        evict(q, now);
        if (q.size() >= max) return false;
        q.addLast(now);
        return true;
    }

    public synchronized int remaining(String key) {
        if (key == null) return 0;
        Deque<Long> q = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        evict(q, System.currentTimeMillis());
        return Math.max(0, max - q.size());
    }

    public int maxRequests() { return max; }
    public long windowSeconds() { return windowSeconds; }

    private void evict(Deque<Long> q, long now) {
        long cutoff = now - windowSeconds * 1000;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
    }
}
