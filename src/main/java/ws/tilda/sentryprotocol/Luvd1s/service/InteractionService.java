package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.repository.InteractionRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InteractionService {

    private final InteractionRepository interactions;
    private final PersonRepository people;

    public InteractionService(InteractionRepository interactions, PersonRepository people) {
        this.interactions = interactions;
        this.people = people;
    }

    public List<Interaction> findByPerson(Person person) {
        return interactions.findByPersonOrderByOccurredAtDesc(person);
    }

    public Interaction log(Interaction interaction) {
        Interaction saved = interactions.save(interaction);
        Person person = interaction.getPerson();
        person.setLastContactedAt(interaction.getOccurredAt());
        people.save(person);
        return saved;
    }
}