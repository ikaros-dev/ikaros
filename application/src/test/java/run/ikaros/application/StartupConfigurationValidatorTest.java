package run.ikaros.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class StartupConfigurationValidatorTest {
    private static final String SECRET = "a-development-secret-with-at-least-32-characters";

    @Test
    void acceptsCompleteCoreConfiguration() {
        assertDoesNotThrow(() -> new StartupConfigurationValidator(environment(Map.of(
            "spring.r2dbc.url", "r2dbc:postgresql://localhost:5432/ikaros",
            "spring.r2dbc.username", "ikaros",
            "spring.r2dbc.password", "password",
            "ikaros.security.jwt.issuer", "ikaros",
            "ikaros.security.jwt.secret", SECRET,
            "ikaros.security.jwt.access-token-ttl", "PT15M",
            "ikaros.security.jwt.refresh-token-ttl", "P30D"))));
    }

    @Test
    void rejectsMissingDatabasePassword() {
        var properties = Map.<String, Object>of(
            "spring.r2dbc.url", "r2dbc:postgresql://localhost:5432/ikaros",
            "spring.r2dbc.username", "ikaros",
            "spring.r2dbc.password", "",
            "ikaros.security.jwt.issuer", "ikaros",
            "ikaros.security.jwt.secret", SECRET,
            "ikaros.security.jwt.access-token-ttl", "PT15M",
            "ikaros.security.jwt.refresh-token-ttl", "P30D");

        assertThrows(IllegalStateException.class, () -> new StartupConfigurationValidator(environment(properties)));
    }

    @Test
    void rejectsPlaceholderJwtSecret() {
        var properties = Map.<String, Object>of(
            "spring.r2dbc.url", "r2dbc:postgresql://localhost:5432/ikaros",
            "spring.r2dbc.username", "ikaros",
            "spring.r2dbc.password", "password",
            "ikaros.security.jwt.issuer", "ikaros",
            "ikaros.security.jwt.secret", "change-this-development-secret-to-a-long-random-value",
            "ikaros.security.jwt.access-token-ttl", "PT15M",
            "ikaros.security.jwt.refresh-token-ttl", "P30D");

        assertThrows(IllegalStateException.class, () -> new StartupConfigurationValidator(environment(properties)));
    }

    private StandardEnvironment environment(Map<String, ?> properties) {
        var environment = new StandardEnvironment();
        var values = new HashMap<String, Object>();
        properties.forEach(values::put);
        environment.getPropertySources().addFirst(new MapPropertySource("test", values));
        return environment;
    }
}
