package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.repository.InteractionRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    private final PersonRepository people;
    private final InteractionRepository interactions;

    public DebugController(PersonRepository people, InteractionRepository interactions) {
        this.people = people;
        this.interactions = interactions;
    }

    @GetMapping("/api/debug")
    public String debug() {
        StringBuilder sb = new StringBuilder();
        sb.append("People: ").append(people.count()).append("\n");
        sb.append("Interactions: ").append(interactions.count()).append("\n");
        interactions.findAll().forEach(i -> {
            sb.append("  - id=").append(i.getId())
              .append(", personId=").append(i.getPerson() != null ? i.getPerson().getId() : "null")
              .append(", type=").append(i.getType())
              .append(", occurredAt=").append(i.getOccurredAt())
              .append("\n");
        });
        return sb.toString();
    }
}