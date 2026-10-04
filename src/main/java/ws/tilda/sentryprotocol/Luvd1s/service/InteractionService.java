package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.InteractionRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InteractionService {

    private final InteractionRepository interactions;
    private final PersonRepository people;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public InteractionService(InteractionRepository interactions,
                              PersonRepository people,
                              UserService userService,
                              AuditLogService auditLogService) {
        this.interactions = interactions;
        this.people = people;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#person)")
    public List<Interaction> findByPerson(Person person) {
        if (person == null || person.getId() == null) return List.of();
        return interactions.findByPersonIdOrderByOccurredAtDesc(person.getId());
    }

    @PreAuthorize("isAuthenticated()")
    public List<Interaction> findAllForCurrentUser() {
        User owner = userService.getCurrentUser();
        if (owner == null) return List.of();

        List<Interaction> all = new ArrayList<>();
        for (Person p : people.findByOwnerOrderByLastNameAscFirstNameAsc(owner)) {
            all.addAll(interactions.findByPersonIdOrderByOccurredAtDesc(p.getId()));
        }
        return all;
    }

    @Transactional
    @PreAuthorize("isAuthenticated() and @ownership.owns(#interaction)")
    public Interaction save(Interaction interaction) {
        boolean isNew = interaction.getId() == null;

        Interaction saved = interactions.save(interaction);
        refreshLastContacted(interaction.getPerson());

        auditLogService.record(
                isNew ? "INTERACTION_CREATE" : "INTERACTION_UPDATE",
                "Interaction",
                saved.getId(),
                (isNew ? "Logged " : "Updated ")
                        + saved.getType() + " with "
                        + saved.getPerson().getFirstName() + " " + saved.getPerson().getLastName()
        );

        return saved;
    }

    @Transactional
    @PreAuthorize("isAuthenticated() and @ownership.owns(#interaction)")
    public void delete(Interaction interaction) {
        if (interaction == null || interaction.getId() == null) return;

        Long id = interaction.getId();
        String target = interaction.getPerson().getFirstName()
                + " " + interaction.getPerson().getLastName();
        Person person = interaction.getPerson();

        interactions.delete(interaction);
        refreshLastContacted(person);

        auditLogService.record(
                "INTERACTION_DELETE",
                "Interaction",
                id,
                "Deleted interaction with " + target
        );
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public boolean deleteById(Long id) {
        User owner = userService.getCurrentUser();
        if (owner == null) return false;

        Interaction interaction = interactions.findById(id).orElse(null);
        if (interaction == null) return false;

        Person person = interaction.getPerson();
        if (person == null || person.getOwner() == null
                || !person.getOwner().getId().equals(owner.getId())) {
            return false;
        }

        String target = person.getFirstName() + " " + person.getLastName();

        interactions.delete(interaction);
        refreshLastContacted(person);

        auditLogService.record(
                "INTERACTION_DELETE",
                "Interaction",
                id,
                "Deleted interaction with " + target
        );

        return true;
    }

    private void refreshLastContacted(Person person) {
        if (person == null || person.getId() == null) return;
        LocalDateTime latest = interactions
                .findByPersonIdOrderByOccurredAtDesc(person.getId())
                .stream()
                .map(Interaction::getOccurredAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        person.setLastContactedAt(latest);
        people.save(person);
    }
}