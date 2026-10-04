package ws.tilda.sentryprotocol.Luvd1s.api.dto;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PersonDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String company,
        String jobTitle,
        LocalDate birthday,
        String notes,
        LocalDateTime lastContactedAt,
        List<TagDto> tags
) {

    public static PersonDto from(Person person) {
        return new PersonDto(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getEmail(),
                person.getPhone(),
                person.getCompany(),
                person.getJobTitle(),
                person.getBirthday(),
                person.getNotes(),
                person.getLastContactedAt(),
                person.getTags().stream()
                        .map(TagDto::from)
                        .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                        .toList()
        );
    }
}