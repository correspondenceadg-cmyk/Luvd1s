package ws.tilda.sentryprotocol.Luvd1s;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * Spins up a real Postgres container once per JVM and wires the Spring
 * datasource to it via @ServiceConnection. Tests inherit from this and
 * add their own @SpringBootTest annotation.
 */
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("luvd1s_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Actuator exposes a `health` endpoint that would try to reach the DB.
        // Test-scope tuning keeps startup fast.
        registry.add("management.endpoints.web.exposure.include", () -> "health");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}