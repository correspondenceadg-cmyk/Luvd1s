package ws.tilda.sentryprotocol.Luvd1s.repository;

import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
}