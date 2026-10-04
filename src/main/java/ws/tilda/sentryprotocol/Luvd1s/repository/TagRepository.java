package ws.tilda.sentryprotocol.Luvd1s.repository;

import ws.tilda.sentryprotocol.Luvd1s.data.Tag;
import ws.tilda.sentryprotocol.Luvd1s.data.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByOwner(User owner);

    Optional<Tag> findByOwnerAndName(User owner, String name);
}