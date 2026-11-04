package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int KEY_BYTES = 32;

    private final UserRepository users;

    public ApiKeyService(UserRepository users) {
        this.users = users;
    }

    public Optional<User> resolve(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return Optional.empty();
        String hash = sha256(rawKey.trim());
        return users.findByApiKeyHash(hash);
    }

    public String generateAndStore(User user) {
        byte[] bytes = new byte[KEY_BYTES];
        RANDOM.nextBytes(bytes);
        String raw = "lvd_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setApiKeyHash(sha256(raw));
        user.setApiKeyPrefix(raw.substring(0, 8));
        user.setApiKeySuffix(raw.substring(raw.length() - 4));
        users.save(user);
        return raw;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}