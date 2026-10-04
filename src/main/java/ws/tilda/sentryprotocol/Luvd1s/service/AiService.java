package ws.tilda.sentryprotocol.Luvd1s.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RestClient client;
    private final String apiKey;
    private final String model;

    public AiService(@Value("${groq.api-key:}") String apiKey,
                     @Value("${groq.model:llama-3.3-70b-versatile}") String model) {
        this.apiKey = apiKey;
        this.model = model;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(20).toMillis());

        this.client = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .requestFactory(factory)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Generic chat completion. Callers are expected to have already scrubbed
     * PII from the messages before calling this. The AiChatService is the
     * only intended caller.
     */
    public Optional<String> chat(List<Map<String, String>> messages) {
        if (!isConfigured()) return Optional.empty();
        if (messages == null || messages.isEmpty()) return Optional.empty();

        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "messages", messages,
                    "max_tokens", 500,
                    "temperature", 0.4
            );

            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) return Optional.empty();

            JsonNode choices = response.path("choices");
            if (!choices.isArray() || choices.isEmpty()) return Optional.empty();

            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) return Optional.empty();

            return Optional.of(content.trim());
        } catch (Exception ex) {
            log.warn("AI chat call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}