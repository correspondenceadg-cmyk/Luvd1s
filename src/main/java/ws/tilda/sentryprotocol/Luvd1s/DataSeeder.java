package ws.tilda.sentryprotocol.Luvd1s;

import ws.tilda.sentryprotocol.Luvd1s.data.*;
import ws.tilda.sentryprotocol.Luvd1s.repository.*;
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

        Tag friend = tag("friend", "#4CAF50");
        Tag work = tag("work", "#2196F3");
        Tag family = tag("family", "#E91E63");
        Tag college = tag("college", "#9C27B0");

        Person alice = person("Alice", "Nguyen", "alice@example.com", "Acme", "Engineer",
                LocalDate.of(1990, 5, 12), friend, work);
        Person bob = person("Bob", "Martinez", "bob@example.com", "Globex", "Designer",
                LocalDate.of(1988, 11, 3), work);
        Person carla = person("Carla", "Okafor", "carla@example.com", "Acme", "PM",
                LocalDate.of(1992, 2, 27), friend, college);
        Person dev = person("Dev", "Patel", "dev@example.com", "Initech", "Founder",
                LocalDate.of(1985, 7, 19), friend, work, college);
        Person elena = person("Elena", "Rossi", "elena@example.com", "Globex", "CTO",
                LocalDate.of(1980, 12, 30), work, family);

        Interaction i = new Interaction();
        i.setPerson(alice);
        i.setType(InteractionType.COFFEE);
        i.setOccurredAt(LocalDateTime.now().minusDays(3));
        i.setSummary("Caught up about her new role.");
        interactions.save(i);
        alice.setLastContactedAt(i.getOccurredAt());
        people.save(alice);
    }

    private Tag tag(String name, String color) {
        Tag t = new Tag();
        t.setName(name);
        t.setColor(color);
        return tags.save(t);
    }

    private Person person(String first, String last, String email, String company,
                          String jobTitle, LocalDate birthday, Tag... personTags) {
        Person p = new Person();
        p.setFirstName(first);
        p.setLastName(last);
        p.setEmail(email);
        p.setCompany(company);
        p.setJobTitle(jobTitle);
        p.setBirthday(birthday);
        for (Tag t : personTags) {
            p.getTags().add(t);
        }
        return people.save(p);
    }
}