package ws.tilda.sentryprotocol.Luvd1s.api;

import ws.tilda.sentryprotocol.Luvd1s.api.dto.PersonDto;
import ws.tilda.sentryprotocol.Luvd1s.api.dto.PersonInput;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/people")
public class PersonApiController {

    private final PersonService personService;

    public PersonApiController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public List<PersonDto> list() {
        return personService.findAll().stream()
                .map(PersonDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PersonDto get(@PathVariable Long id) {
        Person person = personService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
        return PersonDto.from(person);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonDto create(@RequestBody PersonInput input) {
        Person person = new Person();
        applyInput(person, input);
        Person saved = personService.save(person);
        return PersonDto.from(saved);
    }

    @PutMapping("/{id}")
    public PersonDto update(@PathVariable Long id, @RequestBody PersonInput input) {
        Person person = personService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
        applyInput(person, input);
        Person saved = personService.save(person);
        return PersonDto.from(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Person person = personService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
        personService.delete(person);
    }

    private void applyInput(Person person, PersonInput input) {
        if (input.firstName() != null) person.setFirstName(input.firstName());
        if (input.lastName() != null) person.setLastName(input.lastName());
        if (input.email() != null) person.setEmail(input.email());
        if (input.phone() != null) person.setPhone(input.phone());
        if (input.company() != null) person.setCompany(input.company());
        if (input.jobTitle() != null) person.setJobTitle(input.jobTitle());
        if (input.birthday() != null) person.setBirthday(input.birthday());
        if (input.notes() != null) person.setNotes(input.notes());
    }
}