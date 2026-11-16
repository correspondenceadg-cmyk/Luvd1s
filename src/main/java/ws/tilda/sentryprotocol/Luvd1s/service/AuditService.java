package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.AuditLog;
import ws.tilda.sentryprotocol.Luvd1s.repository.AuditLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository repo;
    private final UserService userService;
    private final Counter auditCounter;

    public AuditService(AuditLogRepository repo,
                        UserService userService,
                        MeterRegistry meterRegistry) {
        this.repo = repo;
        this.userService = userService;
        this.auditCounter = Counter.builder("luvd1s_audit_written_total")
                .description("Audit rows written")
                .register(meterRegistry);
    }

    @Transactional
    public void log(String action, String targetType, Long targetId, String details) {
        AuditLog a = new AuditLog();
        a.setCreatedAt(Instant.now());
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setDetails(details);

        var user = userService.getCurrentUser();
        a.setUsername(user != null ? user.getUsername() : "anonymous");

        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            a.setIpAddress(extractIp(req));
            String ua = req.getHeader("User-Agent");
            if (ua != null) a.setUserAgent(ua.length() > 200 ? ua.substring(0, 200) : ua);
        }

        repo.save(a);
        auditCounter.increment();
    }

    private String extractIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        return req.getRemoteAddr();
    }
}
