package run.ikaros.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
    @Test
    void issuesAndVerifiesSeparateAccessAndRefreshTokens() {
        JwtTokenService service = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters", 
            Duration.ofMinutes(15), Duration.ofDays(30));
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        JwtTokenService.TokenPair pair = service.issue(userId, sessionId, List.of("resource.read"));

        assertThat(service.verifyAccess(pair.accessToken()).userId()).isEqualTo(userId);
        assertThat(service.verifyAccess(pair.accessToken()).sessionId()).isEqualTo(sessionId);
        assertThat(service.verifyAccess(pair.accessToken()).permissions()).containsExactly("resource.read");
        assertThat(service.verifyRefresh(pair.refreshToken()).userId()).isEqualTo(userId);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.verifyAccess(pair.refreshToken()))
            .isInstanceOf(RuntimeException.class);
    }
}
