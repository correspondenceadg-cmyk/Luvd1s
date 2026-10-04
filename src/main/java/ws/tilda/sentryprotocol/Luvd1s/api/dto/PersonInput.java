package ws.tilda.sentryprotocol.Luvd1s.api.dto;

import java.time.LocalDate;

public record PersonInput(
        String firstName,
        String lastName,
        String email,
        String phone,
        String company,
        String jobTitle,
        LocalDate birthday,
        String notes
) {
}