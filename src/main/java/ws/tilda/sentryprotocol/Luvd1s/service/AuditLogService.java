package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.AuditLog;
import ws.tilda.sentryprotocol.Luvd1s.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository repository;
    private final ObservabilityService observability;

    public AuditLogService(AuditLogRepository repository,
                           ObservabilityService observability) {
        this.repository = repository;
        this.observability = observability;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, Long targetId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setCreatedAt(Instant.now());
            entry.setUsername(currentUsername());
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setDetails(truncate(details, 500));
            entry.setIpAddress(currentIp());
            entry.setUserAgent(truncate(currentUserAgent(), 200));
            repository.save(entry);
            observability.recordAuditWritten();
        } catch (Exception ex) {
            log.warn("Failed to write audit log for {}: {}", action, ex.getMessage());
        }
    }

    public List<AuditLog> recent() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        if (name == null || "anonymousUser".equals(name)) return null;
        return name;
    }

    private String currentIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return req.getRemoteAddr();
    }

    private String currentUserAgent() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getHeader("User-Agent") : null;
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}