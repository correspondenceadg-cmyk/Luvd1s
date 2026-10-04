package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.AuditLog;
import ws.tilda.sentryprotocol.Luvd1s.repository.AuditLogRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.InteractionRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.PersonRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.TagRepository;
import ws.tilda.sentryprotocol.Luvd1s.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DebugService {

    private final PersonRepository people;
    private final InteractionRepository interactions;
    private final TagRepository tags;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final RecentRequestsBuffer requestBuffer;

    public DebugService(PersonRepository people,
                        InteractionRepository interactions,
                        TagRepository tags,
                        UserRepository users,
                        AuditLogRepository auditLogs,
                        RecentRequestsBuffer requestBuffer) {
        this.people = people;
        this.interactions = interactions;
        this.tags = tags;
        this.users = users;
        this.auditLogs = auditLogs;
        this.requestBuffer = requestBuffer;
    }

    public Map<String, String> jvmStats() {
        Runtime r = Runtime.getRuntime();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        long uptimeSec = runtime.getUptime() / 1000;
        long heapUsedMb = (r.totalMemory() - r.freeMemory()) / 1024 / 1024;
        long heapMaxMb = r.maxMemory() / 1024 / 1024;

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("heapUsed", heapUsedMb + " MB");
        stats.put("heapMax", heapMaxMb + " MB");
        stats.put("processors", String.valueOf(r.availableProcessors()));
        stats.put("threads", String.valueOf(threads.getThreadCount()));
        stats.put("peakThreads", String.valueOf(threads.getPeakThreadCount()));
        stats.put("uptime", formatUptime(uptimeSec));
        return stats;
    }

    public Map<String, Long> rowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", users.count());
        counts.put("people", people.count());
        counts.put("interactions", interactions.count());
        counts.put("tags", tags.count());
        counts.put("auditLogs", auditLogs.count());
        return counts;
    }

    public Map<String, String> systemStats() {
        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("javaVersion", System.getProperty("java.version"));
        stats.put("javaVendor", System.getProperty("java.vendor"));
        stats.put("osName", System.getProperty("os.name"));
        stats.put("osArch", System.getProperty("os.arch"));
        stats.put("timezone", System.getProperty("user.timezone"));
        stats.put("locale", System.getProperty("user.language") + "-" + System.getProperty("user.country"));
        return stats;
    }

    public List<RequestRecord> recentRequests() {
        return requestBuffer.recent();
    }

    public List<AuditLog> recentAudits() {
        return auditLogs.findTop50ByOrderByCreatedAtDesc();
    }

    private String formatUptime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}