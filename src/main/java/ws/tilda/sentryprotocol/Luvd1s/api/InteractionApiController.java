package ws.tilda.sentryprotocol.Luvd1s.api;

import ws.tilda.sentryprotocol.Luvd1s.api.dto.InteractionDto;
import ws.tilda.sentryprotocol.Luvd1s.api.dto.InteractionInput;
import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.InteractionType;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.service.InteractionService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InteractionApiController {

    private final PersonService personService;
    private final InteractionService interactionService;

    public InteractionApiController(PersonService personService,
                                    InteractionService interactionService) {
        this.personService = personService;
        this.interactionService = interactionService;
    }

    @GetMapping("/people/{personId}/interactions")
    public List<InteractionDto> list(@PathVariable Long personId) {
        Person person = personService.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
        return interactionService.findByPerson(person).stream()
                .map(InteractionDto::from)
                .toList();
    }

    @PostMapping("/people/{personId}/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    public InteractionDto create(@PathVariable Long personId,
                                 @RequestBody InteractionInput input) {
        Person person = personService.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));

        if (input.type() == null || input.type().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }

        InteractionType type;
        try {
            type = InteractionType.valueOf(input.type().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown type: " + input.type());
        }

        Interaction interaction = new Interaction();
        interaction.setPerson(person);
        interaction.setType(type);
        interaction.setOccurredAt(input.occurredAt() != null ? input.occurredAt() : LocalDateTime.now());
        interaction.setSummary(input.summary());
        interaction.setFollowUpDate(input.followUpDate());

        Interaction saved = interactionService.save(interaction);
        return InteractionDto.from(saved);
    }

    @DeleteMapping("/interactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        boolean deleted = interactionService.deleteById(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Interaction not found");
        }
    }
}