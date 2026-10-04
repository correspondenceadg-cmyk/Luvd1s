package ws.tilda.sentryprotocol.Luvd1s.service;

import ws.tilda.sentryprotocol.Luvd1s.data.User;
import ws.tilda.sentryprotocol.Luvd1s.repository.UserRepository;
import ws.tilda.sentryprotocol.Luvd1s.security.UsernameValidator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized;
        try {
            normalized = UsernameValidator.normalize(username);
        } catch (IllegalArgumentException ex) {
            throw new UsernameNotFoundException("User not found");
        }

        User user = users.findByUsername(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }

    public User register(String username, String rawPassword, String displayName) {
        String normalized = UsernameValidator.normalize(username);

        if (users.findByUsername(normalized).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User();
        user.setUsername(normalized);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        return users.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return users.findByUsername(UsernameValidator.normalize(username));
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String username = auth.getName();
        if (username == null || "anonymousUser".equals(username)) {
            return null;
        }
        try {
            return users.findByUsername(UsernameValidator.normalize(username)).orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}