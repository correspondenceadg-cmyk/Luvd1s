package ws.tilda.sentryprotocol.Luvd1s.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    List<Interaction> findByPersonOrderByOccurredAtDesc(Person person);
}