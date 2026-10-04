package ws.tilda.sentryprotocol.Luvd1s.service;

import java.time.Instant;

public record RequestRecord(
        Instant timestamp,
        String method,
        String path,
        String username,
        int status,
        long durationMs
) {
}