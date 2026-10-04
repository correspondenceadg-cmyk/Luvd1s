package ws.tilda.sentryprotocol.Luvd1s.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ObservabilityService {

    private final MeterRegistry registry;

    private final Counter personCreated;
    private final Counter personUpdated;
    private final Counter personDeleted;
    private final Counter interactionCreated;
    private final Counter interactionUpdated;
    private final Counter interactionDeleted;
    private final Counter auditWritten;
    private final Counter authFailure;
    private final Timer aiSummaryLatency;

    public ObservabilityService(MeterRegistry registry) {
        this.registry = registry;
        this.personCreated = Counter.builder("luvd1s.person.created")
                .description("People created").register(registry);
        this.personUpdated = Counter.builder("luvd1s.person.updated")
                .description("People updated").register(registry);
        this.personDeleted = Counter.builder("luvd1s.person.deleted")
                .description("People deleted").register(registry);
        this.interactionCreated = Counter.builder("luvd1s.interaction.created")
                .description("Interactions created").register(registry);
        this.interactionUpdated = Counter.builder("luvd1s.interaction.updated")
                .description("Interactions updated").register(registry);
        this.interactionDeleted = Counter.builder("luvd1s.interaction.deleted")
                .description("Interactions deleted").register(registry);
        this.auditWritten = Counter.builder("luvd1s.audit.written")
                .description("Audit entries written").register(registry);
        this.authFailure = Counter.builder("luvd1s.auth.failure")
                .description("Authentication failures").register(registry);
        this.aiSummaryLatency = Timer.builder("luvd1s.ai.summary.latency")
                .description("Latency of AI summary calls")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordPersonCreated() { personCreated.increment(); }
    public void recordPersonUpdated() { personUpdated.increment(); }
    public void recordPersonDeleted() { personDeleted.increment(); }
    public void recordInteractionCreated() { interactionCreated.increment(); }
    public void recordInteractionUpdated() { interactionUpdated.increment(); }
    public void recordInteractionDeleted() { interactionDeleted.increment(); }
    public void recordAuditWritten() { auditWritten.increment(); }
    public void recordAuthFailure() { authFailure.increment(); }

    public void recordAiSummary(Duration duration) {
        aiSummaryLatency.record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Snapshot of HTTP request metrics keyed by URI + method.
     */
    public Map<String, String> httpStats() {
        Map<String, String> stats = new LinkedHashMap<>();

        double totalRequests = 0;
        double totalTime = 0;
        double maxTime = 0;

        for (var meter : registry.find("http.server.requests").meters()) {
            if (meter instanceof Timer timer) {
                totalRequests += timer.count();
                totalTime += timer.totalTime(TimeUnit.MILLISECONDS);
                maxTime = Math.max(maxTime, timer.max(TimeUnit.MILLISECONDS));
            }
        }

        stats.put("totalRequests", String.valueOf((long) totalRequests));
        stats.put("avgLatency", totalRequests > 0
                ? String.format("%.1f ms", totalTime / totalRequests)
                : "—");
        stats.put("maxLatency", maxTime > 0
                ? String.format("%.0f ms", maxTime)
                : "—");
        return stats;
    }

    /**
     * Custom business counters so far.
     */
    public Map<String, String> businessCounters() {
        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("peopleCreated", String.valueOf((long) personCreated.count()));
        stats.put("peopleUpdated", String.valueOf((long) personUpdated.count()));
        stats.put("peopleDeleted", String.valueOf((long) personDeleted.count()));
        stats.put("interactionsCreated", String.valueOf((long) interactionCreated.count()));
        stats.put("interactionsUpdated", String.valueOf((long) interactionUpdated.count()));
        stats.put("interactionsDeleted", String.valueOf((long) interactionDeleted.count()));
        stats.put("auditEntriesWritten", String.valueOf((long) auditWritten.count()));
        stats.put("authFailures", String.valueOf((long) authFailure.count()));

        long aiCount = (long) aiSummaryLatency.count();
        stats.put("aiSummaryCalls", String.valueOf(aiCount));
        if (aiCount > 0) {
            stats.put("aiSummaryAvgMs",
                    String.format("%.0f", aiSummaryLatency.mean(TimeUnit.MILLISECONDS)));
        }
        return stats;
    }

    /**
     * Database connection pool stats from Hikari.
     */
    public Map<String, String> poolStats() {
        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("activeConnections", gaugeValue("hikaricp.connections.active"));
        stats.put("idleConnections", gaugeValue("hikaricp.connections.idle"));
        stats.put("pendingThreads", gaugeValue("hikaricp.connections.pending"));
        stats.put("totalConnections", gaugeValue("hikaricp.connections"));
        return stats;
    }

    public String scrapeUrl() {
        return "/actuator/prometheus";
    }

    private String gaugeValue(String name) {
        var gauge = registry.find(name).gauge();
        if (gauge == null) return "—";
        return String.format("%.0f", gauge.value());
    }
}