package ws.tilda.sentryprotocol.Luvd1s;

import ws.tilda.sentryprotocol.Luvd1s.data.*;
import ws.tilda.sentryprotocol.Luvd1s.repository.*;
import ws.tilda.sentryprotocol.Luvd1s.service.ApiKeyService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final String[] SUMMARIES = {
            "Quick check-in",
            "Discussed project timeline",
            "Caught up over coffee",
            "Followed up on the proposal",
            "Birthday call",
            "Reviewed the contract",
            "Grabbed lunch",
            "Talked about the new role",
            "Networking intro",
            "Sent a thank-you note",
            "Slack thread about hiring",
            "Dropped off a book",
            "Sync on the roadmap",
            "Weekly standup call",
            "Happy hour catch-up"
    };

    private final PersonRepository people;
    private final InteractionRepository interactions;
    private final TagRepository tags;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyService apiKeyService;

    public DataSeeder(PersonRepository people,
                      InteractionRepository interactions,
                      TagRepository tags,
                      UserRepository users,
                      PasswordEncoder passwordEncoder,
                      ApiKeyService apiKeyService) {
        this.people = people;
        this.interactions = interactions;
        this.tags = tags;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.apiKeyService = apiKeyService;
    }

    @Override
    public void run(String... args) {
        User admin = users.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setPasswordHash(passwordEncoder.encode("admin123"));
            u.setDisplayName("Admin");
            return users.save(u);
        });

        if (admin.getApiKey() == null || admin.getApiKey().isBlank()) {
            String key = apiKeyService.generateAndStore(admin);
            System.out.println("========================================================");
            System.out.println("Admin API key (save this): " + key);
            System.out.println("========================================================");
        }

        if (people.count() > 0) return;

        Tag friend = tag(admin, "friend", "#4CAF50");
        Tag work = tag(admin, "work", "#2196F3");
        Tag family = tag(admin, "family", "#E91E63");
        Tag college = tag(admin, "college", "#9C27B0");

        Person alice = person(admin, "Alice", "Nguyen", "alice@example.com", "Acme", "Engineer",
                LocalDate.of(1990, 5, 12), friend, work);
        Person bob = person(admin, "Bob", "Martinez", "bob@example.com", "Globex", "Designer",
                LocalDate.of(1988, 11, 3), work);
        Person carla = person(admin, "Carla", "Okafor", "carla@example.com", "Acme", "PM",
                LocalDate.of(1992, 2, 27), friend, college);
        Person dev = person(admin, "Dev", "Patel", "dev@example.com", "Initech", "Founder",
                LocalDate.of(1985, 7, 19), friend, work, college);
        Person elena = person(admin, "Elena", "Rossi", "elena@example.com", "Globex", "CTO",
                LocalDate.of(1980, 12, 30), work, family);

        Random rng = new Random(42);

        List<Person> all = List.of(alice, bob, carla, dev, elena);
        int[] baseFrequencies = {6, 3, 8, 5, 2};

        for (int idx = 0; idx < all.size(); idx++) {
            Person p = all.get(idx);
            int count = baseFrequencies[idx] + rng.nextInt(6);
            int trend = idx % 2 == 0 ? -1 : 1;

            for (int i = 0; i < count; i++) {
                int daysAgo = rng.nextInt(150) + 1;
                if (trend < 0) {
                    daysAgo = (int) (daysAgo * (1.0 - i * 0.1));
                } else {
                    daysAgo = (int) (daysAgo * (0.3 + i * 0.15));
                }
                daysAgo = Math.max(1, Math.min(170, daysAgo));

                Interaction interaction = new Interaction();
                interaction.setPerson(p);
                interaction.setType(InteractionType.values()[rng.nextInt(InteractionType.values().length)]);
                interaction.setOccurredAt(LocalDateTime.now()
                        .minusDays(daysAgo)
                        .minusHours(rng.nextInt(24))
                        .minusMinutes(rng.nextInt(60)));
                interaction.setSummary(SUMMARIES[rng.nextInt(SUMMARIES.length)]);
                interactions.save(interaction);
            }

            refreshLastContacted(p);
            people.save(p);
        }
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
    }

    private Tag tag(User owner, String name, String color) {
        Tag t = new Tag();
        t.setOwner(owner);
        t.setName(name);
        t.setColor(color);
        return tags.save(t);
    }

    private Person person(User owner, String first, String last, String email,
                          String company, String jobTitle, LocalDate birthday,
                          Tag... personTags) {
        Person p = new Person();
        p.setOwner(owner);
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