package ws.tilda.sentryprotocol.Luvd1s;

import ws.tilda.sentryprotocol.Luvd1s.data.*;
import ws.tilda.sentryprotocol.Luvd1s.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PersonRepository people;
    private final InteractionRepository interactions;
    private final TagRepository tags;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    @Value("${admin.seed.username:}")
    private String adminUsername;

    @Value("${admin.seed.password:}")
    private String adminPassword;

    public DataSeeder(PersonRepository people,
                      InteractionRepository interactions,
                      TagRepository tags,
                      UserRepository users,
                      PasswordEncoder passwordEncoder,
                      Environment env) {
        this.people = people;
        this.interactions = interactions;
        this.tags = tags;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        boolean isProd = "prod".equals(env.getActiveProfiles().length > 0
                ? env.getActiveProfiles()[0] : "default");

        if (isProd) {
            // Production: only seed if explicit admin credentials are set.
            if (adminUsername.isBlank() || adminPassword.isBlank()) return;
        } else {
            // Dev: default credentials if not overridden.
            if (adminUsername.isBlank()) adminUsername = "admin";
            if (adminPassword.isBlank()) adminPassword = "admin123";
        }

        if (users.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setDisplayName("Admin");
            admin.setRole("ADMIN");
            users.save(admin);
            System.out.println("Created admin user: " + adminUsername);
        }

        if (people.count() > 0) return;

        Tag friend = tag(adminUsername, "friend", "#4CAF50");
        Tag work = tag(adminUsername, "work", "#2196F3");
        Tag family = tag(adminUsername, "family", "#E91E63");
        Tag college = tag(adminUsername, "college", "#9C27B0");

        Person alice = person(adminUsername, "Alice", "Nguyen", "alice@example.com",
                "Acme", "Engineer", LocalDate.of(1990, 5, 12), friend, work);
        Person bob = person(adminUsername, "Bob", "Martinez", "bob@example.com",
                "Globex", "Designer", LocalDate.of(1988, 11, 3), work);
        person(adminUsername, "Carla", "Okafor", "carla@example.com",
                "Acme", "PM", LocalDate.of(1992, 2, 27), friend, college);
        person(adminUsername, "Dev", "Patel", "dev@example.com",
                "Initech", "Founder", LocalDate.of(1985, 7, 19), friend, work, college);
        person(adminUsername, "Elena", "Rossi", "elena@example.com",
                "Globex", "CTO", LocalDate.of(1980, 12, 30), work, family);

        Interaction i = new Interaction();
        i.setPerson(alice);
        i.setType(InteractionType.COFFEE);
        i.setOccurredAt(LocalDateTime.now().minusDays(3));
        i.setSummary("Caught up about her new role.");
        interactions.save(i);
        alice.setLastContactedAt(i.getOccurredAt());
        people.save(alice);
    }

    private Tag tag(String ownerUsername, String name, String color) {
        User owner = users.findByUsername(ownerUsername).orElseThrow();
        Tag t = new Tag();
        t.setOwner(owner);
        t.setName(name);
        t.setColor(color);
        return tags.save(t);
    }

    private Person person(String ownerUsername, String first, String last,
                          String email, String company, String jobTitle,
                          LocalDate birthday, Tag... personTags) {
        User owner = users.findByUsername(ownerUsername).orElseThrow();
        Person p = new Person();
        p.setOwner(owner);
        p.setFirstName(first);
        p.setLastName(last);
        p.setEmail(email);
        p.setCompany(company);
        p.setJobTitle(jobTitle);
        p.setBirthday(birthday);
        for (Tag t : personTags) p.getTags().add(t);
        return people.save(p);
    }
}