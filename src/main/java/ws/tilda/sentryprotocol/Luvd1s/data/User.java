package ws.tilda.sentryprotocol.Luvd1s.data;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String displayName;

    @Column(unique = true, length = 64)
    private String apiKeyHash;

    @Column(length = 8)
    private String apiKeyPrefix;

    @Column(length = 8)
    private String apiKeySuffix;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    // getters and setters — add the two new fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }
    public String getApiKeySuffix() { return apiKeySuffix; }
    public void setApiKeySuffix(String apiKeySuffix) { this.apiKeySuffix = apiKeySuffix; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}