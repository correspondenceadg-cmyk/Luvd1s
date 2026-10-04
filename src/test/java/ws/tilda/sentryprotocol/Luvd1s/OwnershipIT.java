package ws.tilda.sentryprotocol.Luvd1s;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.service.PersonService;
import ws.tilda.sentryprotocol.Luvd1s.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OwnershipIT extends IntegrationTestBase {

    @Autowired
    PersonService personService;

    @Autowired
    UserService userService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotAllowOneUserToSeeAnotherUsersPeople() {
        User alice = registerAndAuthenticate("it-alice-" + System.nanoTime());
        Person alicePerson = new Person();
        alicePerson.setFirstName("Alice");
        alicePerson.setLastName("Contact");
        personService.save(alicePerson);

        User bob = registerAndAuthenticate("it-bob-" + System.nanoTime());
        assertThat(personService.findAll()).isEmpty();

        // Bob tries to read Alice's person by ID
        Long alicePersonId = alicePerson.getId();
        assertThat(personService.findById(alicePersonId)).isEmpty();

        // Alice can still see it
        authenticate(alice);
        assertThat(personService.findById(alicePersonId)).isPresent();
    }

    @Test
    void shouldRejectDeleteOfAnotherUsersPerson() {
        User alice = registerAndAuthenticate("it-alice-del-" + System.nanoTime());
        Person alicePerson = new Person();
        alicePerson.setFirstName("Alice");
        alicePerson.setLastName("Deletable");
        Person saved = personService.save(alicePerson);
        Long alicePersonId = saved.getId();

        User bob = registerAndAuthenticate("it-bob-del-" + System.nanoTime());

        assertThatThrownBy(() -> personService.delete(saved))
                .isInstanceOf(AccessDeniedException.class);

        // Alice's person is still there
        authenticate(alice);
        assertThat(personService.findById(alicePersonId)).isPresent();
    }

    private User registerAndAuthenticate(String username) {
        User user = userService.register(username, "password123", username);
        authenticate(user);
        return user;
    }

    private void authenticate(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}