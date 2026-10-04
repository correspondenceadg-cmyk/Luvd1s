package ws.tilda.sentryprotocol.Luvd1s.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class RecentRequestsBuffer {

    private static final int MAX = 50;

    private final Deque<RequestRecord> buffer = new ArrayDeque<>();

    public synchronized void add(RequestRecord record) {
        buffer.addFirst(record);
        while (buffer.size() > MAX) {
            buffer.removeLast();
        }
    }

    public synchronized List<RequestRecord> recent() {
        return new ArrayList<>(buffer);
    }
}