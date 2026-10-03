package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {

    private final PersonRepository people;

    public PersonService(PersonRepository people) {
        this.people = people;
    }

    public List<Person> findAll() {
        return people.findAll();
    }

    public Person save(Person person) {
        return people.save(person);
    }

    public void delete(Person person) {
        people.delete(person);
    }
}