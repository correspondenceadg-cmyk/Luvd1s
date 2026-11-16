package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.InteractionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InteractionService {

    private final InteractionRepository interactions;
    private final PersonService personService;
    private final UserService userService;
    private final AuditService auditService;

    public InteractionService(InteractionRepository interactions,
                              PersonService personService,
                              UserService userService,
                              AuditService auditService) {
        this.interactions = interactions;
        this.personService = personService;
        this.userService = userService;
        this.auditService = auditService;
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#person)")
    public List<Interaction> findByPerson(Person person) {
        return interactions.findByPersonIdOrderByOccurredAtDesc(person.getId());
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#interaction.person)")
    @Transactional
    public Interaction save(Interaction interaction) {
        boolean isNew = interaction.getId() == null;
        Interaction saved = interactions.save(interaction);
        Person p = saved.getPerson();
        if (p != null) {
            p.setLastContactedAt(saved.getOccurredAt());
            personService.saveAsOwner(p, p.getOwner());
        }
        auditService.log(isNew ? "CREATE" : "UPDATE", "Interaction", saved.getId(), null);
        return saved;
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#interaction)")
    @Transactional
    public void delete(Interaction interaction) {
        interactions.delete(interaction);
        auditService.log("DELETE", "Interaction", interaction.getId(), null);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public boolean deleteById(Long id) {
        return interactions.findById(id).map(i -> {
            if (!userService.getCurrentUser().getId().equals(i.getPerson().getOwner().getId())) {
                return false;
            }
            interactions.delete(i);
            auditService.log("DELETE", "Interaction", i.getId(), null);
            return true;
        }).orElse(false);
    }

    @PreAuthorize("isAuthenticated()")
    public List<Interaction> findAllForCurrentUser() {
        User current = userService.getCurrentUser();
        if (current == null) return List.of();
        return personService.findAll().stream()
                .flatMap(p -> interactions.findByPersonIdOrderByOccurredAtDesc(p.getId()).stream())
                .toList();
    }
}
