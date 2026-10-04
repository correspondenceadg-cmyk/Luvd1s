package ws.tilda.sentryprotocol.Luvd1s.repository;

import ws.tilda.sentryprotocol.Luvd1s.data.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop50ByOrderByCreatedAtDesc();
}