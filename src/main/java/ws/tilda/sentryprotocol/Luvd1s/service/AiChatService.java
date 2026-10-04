package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.Interaction;
import ws.tilda.sentryprotocol.Luvd1s.data.Person;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private static final String SESSION_FLAG = "luvd1s.ai.enabled";

    private final AiService aiService;
    private final PersonService personService;
    private final InteractionService interactionService;
    private final PiiScrubber scrubber;
    private final UserService userService;
    private final RateLimiter rateLimiter;

    public AiChatService(AiService aiService,
                         PersonService personService,
                         InteractionService interactionService,
                         PiiScrubber scrubber,
                         UserService userService,
                         RateLimiter rateLimiter) {
        this.aiService = aiService;
        this.personService = personService;
        this.interactionService = interactionService;
        this.scrubber = scrubber;
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    public boolean isEnabledForSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) return false;
        Object flag = session.getAttribute(SESSION_FLAG);
        return Boolean.TRUE.equals(flag);
    }

    public void enableForSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SESSION_FLAG, Boolean.TRUE);
        }
    }

    public int remainingRequests() {
        return rateLimiter.remaining(currentUserKey());
    }

    public int maxRequests() {
        return rateLimiter.maxRequests();
    }

    public String freeChat(String userMessage) {
        if (!aiService.isConfigured()) {
            return "AI is not configured on this server.";
        }
        if (!checkRateLimit()) {
            return rateLimitMessage();
        }

        String clean = scrubber.scrub(userMessage);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", clean)
        );
        return aiService.chat(messages).orElse("I couldn't reach the AI service just now.");
    }

    public String whoShouldIReachOutTo() {
        if (!aiService.isConfigured()) {
            return "AI is not configured on this server.";
        }
        if (!checkRateLimit()) {
            return rateLimitMessage();
        }

        List<Person> people = personService.findAll();
        if (people.isEmpty()) {
            return "You don't have any contacts yet.";
        }

        LocalDate today = LocalDate.now();
        List<String> lines = new ArrayList<>();
        for (Person p : people) {
            long days = p.getLastContactedAt() == null
                    ? -1
                    : ChronoUnit.DAYS.between(p.getLastContactedAt().toLocalDate(), today);

            String relationship = describeRelationship(p);
            String cadence = days < 0
                    ? "never contacted"
                    : days + " days since last contact";

            lines.add(scrubber.scrub(
                    "Contact #" + p.getId() + " — " + relationship + ", " + cadence,
                    p));
        }

        String context = String.join("\n", lines);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content",
                        "Based on the following list of contacts with no identifying " +
                        "information, recommend which 3 to reach out to first and why. " +
                        "Reply as a short prioritised list.\n\n" + context)
        );

        return aiService.chat(messages).orElse("I couldn't reach the AI service just now.");
    }

    public String summarizeRecentActivity() {
        if (!aiService.isConfigured()) {
            return "AI is not configured on this server.";
        }
        if (!checkRateLimit()) {
            return rateLimitMessage();
        }

        List<Interaction> all = interactionService.findAllForCurrentUser();
        if (all.isEmpty()) {
            return "You don't have any interactions logged yet.";
        }

        List<Interaction> recent = all.stream()
                .sorted(Comparator.comparing(Interaction::getOccurredAt).reversed())
                .limit(15)
                .toList();

        StringBuilder sb = new StringBuilder();
        for (Interaction i : recent) {
            String type = i.getType() != null ? i.getType().name() : "UNKNOWN";
            String when = i.getOccurredAt() != null ? i.getOccurredAt().toLocalDate().toString() : "unknown";
            String summary = i.getSummary() != null ? i.getSummary() : "";

            String scrubbed = scrubber.scrub(summary, i.getPerson());
            sb.append("- ").append(when).append(" [").append(type).append("]: ")
              .append(scrubbed).append("\n");
        }

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content",
                        "Summarise the following recent interaction log in 3 short bullet points. " +
                        "Do not guess at identities.\n\n" + sb)
        );

        return aiService.chat(messages).orElse("I couldn't reach the AI service just now.");
    }

    private boolean checkRateLimit() {
        return rateLimiter.tryAcquire(currentUserKey());
    }

    private String currentUserKey() {
        var user = userService.getCurrentUser();
        if (user == null) return null;
        return user.getUsername();
    }

    private String rateLimitMessage() {
        long minutes = rateLimiter.windowSeconds() / 60;
        return "You've hit the AI rate limit (" + rateLimiter.maxRequests()
                + " requests per " + minutes + " minutes). Try again later.";
    }

    private String describeRelationship(Person p) {
        StringBuilder sb = new StringBuilder();
        if (p.getJobTitle() != null && !p.getJobTitle().isBlank()) {
            sb.append(p.getJobTitle());
        }
        if (p.getCompany() != null && !p.getCompany().isBlank()) {
            if (sb.length() > 0) sb.append(" at ");
            sb.append(p.getCompany());
        }
        if (p.getTags() != null && !p.getTags().isEmpty()) {
            String tagNames = p.getTags().stream()
                    .map(t -> t.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            if (!tagNames.isBlank()) {
                if (sb.length() > 0) sb.append(". ");
                sb.append("Tags: ").append(tagNames);
            }
        }
        return sb.length() > 0 ? sb.toString() : "no details";
    }

    private String systemPrompt() {
        return "You are an assistant embedded in a personal CRM application. " +
                "Help the user reason about their relationships and interactions. " +
                "Be concise. You do not have access to personal information — " +
                "you only see what the user shares or what the app has redacted. " +
                "Never invent names or contact details.";
    }
}