package run.ikaros.application;

import java.time.Duration;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Fails startup when core deployment configuration is absent or unsafe. */
@Component
public final class StartupConfigurationValidator {
    private static final List<String> REQUIRED_PROPERTIES = List.of(
        "spring.r2dbc.url",
        "spring.r2dbc.username",
        "spring.r2dbc.password",
        "ikaros.security.jwt.issuer",
        "ikaros.security.jwt.secret",
        "ikaros.security.jwt.access-token-ttl",
        "ikaros.security.jwt.refresh-token-ttl"
    );
    private static final String PLACEHOLDER_SECRET = "change-this-development-secret-to-a-long-random-value";

    private final Environment environment;

    public StartupConfigurationValidator(Environment environment) {
        this.environment = environment;
        validate();
    }

    private void validate() {
        REQUIRED_PROPERTIES.forEach(this::requireText);
        requireR2dbcPostgresUrl();
        requireSafeJwtSecret();
        requireDuration("ikaros.security.jwt.access-token-ttl");
        requireDuration("ikaros.security.jwt.refresh-token-ttl");
    }

    private void requireText(String property) {
        if (text(property).isEmpty()) {
            throw invalid(property, "must be configured");
        }
    }

    private void requireR2dbcPostgresUrl() {
        String url = text("spring.r2dbc.url");
        if (!url.startsWith("r2dbc:postgresql://")) {
            throw invalid("spring.r2dbc.url", "must use the PostgreSQL R2DBC driver");
        }
    }

    private void requireSafeJwtSecret() {
        String secret = text("ikaros.security.jwt.secret");
        if (PLACEHOLDER_SECRET.equals(secret) || secret.length() < 32) {
            throw invalid("ikaros.security.jwt.secret", "must be a non-placeholder value of at least 32 characters");
        }
    }

    private void requireDuration(String property) {
        try {
            Duration.parse(text(property));
        } catch (RuntimeException exception) {
            throw invalid(property, "must be an ISO-8601 duration");
        }
    }

    private String text(String property) {
        String value = environment.getProperty(property);
        return value == null ? "" : value.trim();
    }

    private IllegalStateException invalid(String property, String reason) {
        return new IllegalStateException("Startup configuration invalid: " + property + " " + reason);
    }
}
