package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int KEY_BYTES = 32;

    private final UserRepository users;

    public ApiKeyService(UserRepository users) {
        this.users = users;
    }

    public Optional<User> resolve(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();
        return users.findByApiKey(apiKey);
    }

    public String generateAndStore(User user) {
        byte[] bytes = new byte[KEY_BYTES];
        RANDOM.nextBytes(bytes);
        String key = "lvd_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.setApiKey(key);
        users.save(user);
        return key;
    }
}