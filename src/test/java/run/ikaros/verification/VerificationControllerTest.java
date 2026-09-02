package run.ikaros.verification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.ikaros.identity.SecurityVerificationLevel;

/** 验证 Email OTP 控制器的 HTTP 合约。 */
class VerificationControllerTest {
    private EmailOtpVerificationProvider provider;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        provider = mock(EmailOtpVerificationProvider.class);
        client = WebTestClient.bindToController(new VerificationController(provider)).build();
    }

    @Test
    void exposesIssueVerifyAndCancelEndpoints() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        when(provider.issue(any(), any())).thenReturn(Mono.just(new VerificationChallengeView(challengeId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, now.plusSeconds(300),
            VerificationChallengeStatus.ISSUED)));
        when(provider.verify(any(), any(), any())).thenReturn(Mono.just(new VerificationResult(challengeId,
            VerificationMethod.EMAIL_OTP, SecurityVerificationLevel.SVL_1, userId, now, now.plusSeconds(300))));
        when(provider.cancel(userId, challengeId)).thenReturn(Mono.empty());

        client.post().uri("/api/security/verification-challenges").header("X-Ikaros-Actor-Id", userId.toString())
            .bodyValue(Map.of("purpose", "LOGIN_STEP_UP", "targetReference", "session-1"))
            .exchange().expectStatus().isAccepted();
        client.post().uri("/api/security/verification-challenges/{challengeId}/verify", challengeId)
            .header("X-Ikaros-Actor-Id", userId.toString()).bodyValue(Map.of("code", "123456"))
            .exchange().expectStatus().isOk();
        client.delete().uri("/api/security/verification-challenges/{challengeId}", challengeId)
            .header("X-Ikaros-Actor-Id", userId.toString()).exchange().expectStatus().isNoContent();
        verify(provider).cancel(userId, challengeId);
    }
}
