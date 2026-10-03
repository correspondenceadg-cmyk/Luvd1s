package ws.tilda.sentryprotocol.Luvd1s;

import ws.tilda.sentryprotocol.Luvd1s.data.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PersonRepository people;
    private final InteractionRepository interactions;
    private final TagRepository tags;

    public DataSeeder(PersonRepository people,
                      InteractionRepository interactions,
                      TagRepository tags) {
        this.people = people;
        this.interactions = interactions;
        this.tags = tags;
    }

    @Override
    public void run(String... args) {
        if (people.count() > 0) return;

        Tag friend = new Tag(); friend.setName("friend"); friend.setColor("#4CAF50");
        Tag work = new Tag(); work.setName("work"); work.setColor("#2196F3");
        tags.save(friend);
        tags.save(work);

        Person alice = new Person();
        alice.setFirstName("Alice");
        alice.setLastName("Nguyen");
        alice.setEmail("alice@example.com");
        alice.setCompany("Acme");
        alice.setJobTitle("Engineer");
        alice.setBirthday(LocalDate.of(1990, 5, 12));
        alice.getTags().add(friend);
        people.save(alice);

        Person bob = new Person();
        bob.setFirstName("Bob");
        bob.setLastName("Martinez");
        bob.setEmail("bob@example.com");
        bob.setCompany("Globex");
        bob.setJobTitle("Designer");
        bob.getTags().add(work);
        people.save(bob);

        Interaction i = new Interaction();
        i.setPerson(alice);
        i.setType(InteractionType.COFFEE);
        i.setOccurredAt(LocalDateTime.now().minusDays(3));
        i.setSummary("Caught up about her new role.");
        interactions.save(i);

        alice.setLastContactedAt(i.getOccurredAt());
        people.save(alice);
    }
}