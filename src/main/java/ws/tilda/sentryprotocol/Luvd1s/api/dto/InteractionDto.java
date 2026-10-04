package ws.tilda.sentryprotocol.Luvd1s.api.dto;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InteractionDto(
        Long id,
        Long personId,
        String type,
        LocalDateTime occurredAt,
        String summary,
        LocalDate followUpDate
) {

    public static InteractionDto from(Interaction interaction) {
        return new InteractionDto(
                interaction.getId(),
                interaction.getPerson() != null ? interaction.getPerson().getId() : null,
                interaction.getType() != null ? interaction.getType().name() : null,
                interaction.getOccurredAt(),
                interaction.getSummary(),
                interaction.getFollowUpDate()
        );
    }
}