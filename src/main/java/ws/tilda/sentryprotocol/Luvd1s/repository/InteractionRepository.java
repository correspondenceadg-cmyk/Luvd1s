package ws.tilda.sentryprotocol.Luvd1s.repository;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    @Query("SELECT i FROM Interaction i WHERE i.person.id = :personId ORDER BY i.occurredAt DESC")
    List<Interaction> findByPersonIdOrderByOccurredAtDesc(@Param("personId") Long personId);
}