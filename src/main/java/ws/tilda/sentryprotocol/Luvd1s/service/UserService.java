package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.UserRepository;
import ws.tilda.sentryprotocol.Luvd1s.security.UsernameValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String name = auth.getName();
        if (name == null || "anonymousUser".equals(name)) return null;
        return users.findByUsername(name).orElse(null);
    }

    @Transactional
    public User register(String rawUsername, String rawPassword) {
        String username = UsernameValidator.normalize(rawUsername);
        if (users.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        User u = new User();
        u.setUsername(username);
        u.setDisplayName(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole("USER");
        return users.save(u);
    }
}
