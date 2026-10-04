package ws.tilda.sentryprotocol.Luvd1s;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.service.AuditLogService;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PersonServiceIT extends IntegrationTestBase {

    @Autowired
    PersonService personService;

    @Autowired
    UserService userService;

    @Autowired
    AuditLogService auditLogService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Register a unique user for this test run and authenticate as them.
        String username = "it-person-" + System.nanoTime();
        testUser = userService.register(username, "password123", "Integration Test");

        var auth = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldCreateAndRetrievePerson() {
        Person person = new Person();
        person.setFirstName("Ada");
        person.setLastName("Lovelace");
        person.setEmail("ada@example.com");
        person.setCompany("Analytical Engines Ltd");

        Person saved = personService.save(person);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOwner().getId()).isEqualTo(testUser.getId());

        List<Person> all = personService.findAll();
        assertThat(all).extracting(Person::getLastName).contains("Lovelace");
    }

    @Test
    void shouldUpdateExistingPerson() {
        Person person = new Person();
        person.setFirstName("Grace");
        person.setLastName("Hopper");
        Person saved = personService.save(person);

        saved.setJobTitle("Rear Admiral");
        Person updated = personService.save(saved);

        assertThat(updated.getJobTitle()).isEqualTo("Rear Admiral");

        List<Person> all = personService.findAll();
        assertThat(all).hasSize(1);
    }

    @Test
    void shouldDeletePersonAndRecordAuditEntry() {
        long auditBefore = auditLogService.recent().size();

        Person person = new Person();
        person.setFirstName("Alan");
        person.setLastName("Turing");
        Person saved = personService.save(person);

        personService.delete(saved);

        List<Person> all = personService.findAll();
        assertThat(all).isEmpty();

        var audits = auditLogService.recent();
        assertThat(audits.size()).isGreaterThan((int) auditBefore);
        assertThat(audits.get(0).getAction()).isEqualTo("PERSON_DELETE");
    }
}