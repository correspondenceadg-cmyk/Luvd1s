package ws.tilda.sentryprotocol.Luvd1s.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class RecentRequestsBuffer {

    private static final int MAX = 50;
    private final Deque<RequestRecord> ring = new ArrayDeque<>(MAX);

    public synchronized void add(RequestRecord r) {
        if (ring.size() >= MAX) ring.pollFirst();
        ring.addLast(r);
    }

    public synchronized List<RequestRecord> recent() {
        return List.copyOf(ring);
    }
}
