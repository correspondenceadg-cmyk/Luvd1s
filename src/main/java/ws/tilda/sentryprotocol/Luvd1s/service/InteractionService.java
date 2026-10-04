package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.InteractionRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InteractionService {

    private final InteractionRepository interactions;
    private final PersonRepository people;
    private final UserService userService;

    public InteractionService(InteractionRepository interactions,
                              PersonRepository people,
                              UserService userService) {
        this.interactions = interactions;
        this.people = people;
        this.userService = userService;
    }

    public List<Interaction> findByPerson(Person person) {
        if (person == null || person.getId() == null) return List.of();
        User owner = userService.getCurrentUser();
        if (owner == null) return List.of();
        if (person.getOwner() == null || !person.getOwner().getId().equals(owner.getId())) {
            return List.of();
        }
        return interactions.findByPersonIdOrderByOccurredAtDesc(person.getId());
    }

    @Transactional
    public Interaction save(Interaction interaction) {
        User owner = userService.getCurrentUser();
        if (owner == null) throw new IllegalStateException("Not logged in");

        Person person = interaction.getPerson();
        if (person == null || person.getOwner() == null
                || !person.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Not authorized");
        }

        Interaction saved = interactions.save(interaction);
        refreshLastContacted(person);
        return saved;
    }

    @Transactional
    public void delete(Interaction interaction) {
        if (interaction == null) return;
        User owner = userService.getCurrentUser();
        if (owner == null) return;

        Person person = interaction.getPerson();
        if (person == null || person.getOwner() == null
                || !person.getOwner().getId().equals(owner.getId())) {
            return;
        }

        interactions.delete(interaction);
        refreshLastContacted(person);
    }

    @Transactional
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

        interactions.delete(interaction);
        refreshLastContacted(person);
        return true;
    }

    private void refreshLastContacted(Person person) {
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