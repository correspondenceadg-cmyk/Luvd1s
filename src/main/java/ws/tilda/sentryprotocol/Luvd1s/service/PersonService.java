package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PersonService {

    private final PersonRepository people;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public PersonService(PersonRepository people,
                         UserService userService,
                         AuditLogService auditLogService) {
        this.people = people;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Person> findAll() {
        User owner = userService.getCurrentUser();
        if (owner == null) return List.of();
        return people.findByOwnerOrderByLastNameAscFirstNameAsc(owner);
    }

    @PreAuthorize("isAuthenticated()")
    public Optional<Person> findById(Long id) {
        User owner = userService.getCurrentUser();
        if (owner == null) return Optional.empty();
        return people.findByIdAndOwner(id, owner);
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#person)")
    public Person save(Person person) {
        User owner = userService.getCurrentUser();
        if (owner == null) throw new IllegalStateException("Not logged in");

        boolean isNew = person.getId() == null;
        if (person.getOwner() == null) {
            person.setOwner(owner);
        }

        Person saved = people.save(person);

        auditLogService.record(
                isNew ? "PERSON_CREATE" : "PERSON_UPDATE",
                "Person",
                saved.getId(),
                (isNew ? "Created " : "Updated ") + saved.getFirstName() + " " + saved.getLastName()
        );

        return saved;
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#person)")
    public void delete(Person person) {
        if (person == null || person.getId() == null) return;

        String name = person.getFirstName() + " " + person.getLastName();
        Long id = person.getId();

        people.delete(person);

        auditLogService.record("PERSON_DELETE", "Person", id, "Deleted " + name);
    }
}