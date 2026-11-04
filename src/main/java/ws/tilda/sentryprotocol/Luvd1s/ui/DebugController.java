package ws.tilda.sentryprotocol.Luvd1s.ui;

import ws.tilda.sentryprotocol.Luvd1s.repository.InteractionRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    private final PersonRepository people;
    private final InteractionRepository interactions;
    private final UserRepository users;

    public DebugController(PersonRepository people,
                           InteractionRepository interactions,
                           UserRepository users) {
        this.people = people;
        this.interactions = interactions;
        this.users = users;
    }

    @GetMapping("/api/debug")
    @PreAuthorize("hasRole('ADMIN')")
    public String debug() {
        StringBuilder sb = new StringBuilder();
        sb.append("Users: ").append(users.count()).append("\n");
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