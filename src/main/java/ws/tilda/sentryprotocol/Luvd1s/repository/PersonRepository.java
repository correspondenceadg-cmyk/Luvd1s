package ws.tilda.sentryprotocol.Luvd1s.repository;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByOwnerOrderByLastNameAscFirstNameAsc(User owner);

    Optional<Person> findByIdAndOwner(Long id, User owner);
}