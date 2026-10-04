package ws.tilda.sentryprotocol.Luvd1s.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InteractionInput(
        String type,
        LocalDateTime occurredAt,
        String summary,
        LocalDate followUpDate
) {
}