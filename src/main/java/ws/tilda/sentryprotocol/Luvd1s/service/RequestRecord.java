package ws.tilda.sentryprotocol.Luvd1s.service;

import java.time.Instant;

public record RequestRecord(
        Instant at,
        String method,
        String path,
        String user,
        int status,
        long durationMs
) {}
