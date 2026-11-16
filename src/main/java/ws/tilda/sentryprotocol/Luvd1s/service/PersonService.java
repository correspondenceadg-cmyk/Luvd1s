package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PersonService {

    private final PersonRepository people;
    private final UserService userService;

    public PersonService(PersonRepository people, UserService userService) {
        this.people = people;
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Person> findAll() {
        User current = userService.getCurrentUser();
        if (current == null) return List.of();
        return people.findByOwnerOrderByLastNameAscFirstNameAsc(current);
    }

    @PreAuthorize("isAuthenticated()")
    public Optional<Person> findById(Long id) {
        User current = userService.getCurrentUser();
        if (current == null) return Optional.empty();
        return people.findByIdAndOwner(id, current);
    }

    @PreAuthorize("isAuthenticated() and (#person.id == null or @ownership.owns(#person))")
    @Transactional
    public Person save(Person person) {
        if (person.getId() == null) {
            person.setOwner(userService.getCurrentUser());
        }
        return people.save(person);
    }

    @PreAuthorize("isAuthenticated() and @ownership.owns(#person)")
    @Transactional
    public void delete(Person person) {
        people.delete(person);
    }

    @Transactional
    public Person saveAsOwner(Person person, User owner) {
        person.setOwner(owner);
        return people.save(person);
    }
}
