package run.ikaros.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.verification.VerificationPurpose;

class JwtTokenServiceTest {
    @Test
    void issuesAndVerifiesSeparateAccessAndRefreshTokens() {
        JwtTokenService service = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters", 
            Duration.ofMinutes(15), Duration.ofDays(30));
        UUID userId = UUID.randomUUID();
        JwtTokenService.TokenPair pair = service.issue(userId, 3L, List.of("resource.read"));

        assertThat(service.verifyAccess(pair.accessToken()).userId()).isEqualTo(userId);
        assertThat(service.verifyAccess(pair.accessToken()).securityVersion()).isEqualTo(3L);
        assertThat(service.verifyAccess(pair.accessToken()).tokenId()).isNotNull();
        assertThat(service.verifyAccess(pair.accessToken()).permissions()).containsExactly("resource.read");
        assertThat(service.verifyRefresh(pair.refreshToken()).userId()).isEqualTo(userId);
        assertThat(service.verifyAccess(pair.accessToken()).tokenId())
            .isNotEqualTo(service.verifyRefresh(pair.refreshToken()).tokenId());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.verifyAccess(pair.refreshToken()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void issuesVerificationGrantWithJtiAndPurposeClaims() {
        JwtTokenService service = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
            Duration.ofMinutes(15), Duration.ofDays(30));
        UUID userId = UUID.randomUUID();
        Instant verifiedAt = Instant.now();
        String grant = service.issueVerificationGrant(userId, 4L, VerificationPurpose.LOGIN_STEP_UP,
            "resource:" + UUID.randomUUID(), 1, verifiedAt, verifiedAt.plusSeconds(300));

        JwtTokenService.VerificationGrantClaims claims = service.verifyVerificationGrant(grant);

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.grantId()).isNotNull();
        assertThat(claims.securityVersion()).isEqualTo(4L);
        assertThat(claims.purpose()).isEqualTo(VerificationPurpose.LOGIN_STEP_UP);
        assertThat(claims.achievedSvl()).isEqualTo(1);
    }
}
