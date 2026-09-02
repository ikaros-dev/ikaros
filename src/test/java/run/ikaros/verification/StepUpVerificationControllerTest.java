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

/** 验证会话 Step-up 的 HTTP 合约。 */
class StepUpVerificationControllerTest {
    private StepUpVerificationService stepUpService;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        stepUpService = mock(StepUpVerificationService.class);
        client = WebTestClient.bindToController(new StepUpVerificationController(stepUpService)).build();
    }

    @Test
    void exposesSessionBoundIssueAndVerificationEndpoints() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        when(stepUpService.issueEmailOtp(userId, sessionId)).thenReturn(Mono.just(new VerificationChallengeView(challengeId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, now.plusSeconds(300),
            VerificationChallengeStatus.ISSUED)));
        when(stepUpService.verifyEmailOtp(any(), any(), any(), any())).thenReturn(Mono.just(new VerificationResult(
            challengeId, VerificationMethod.EMAIL_OTP, SecurityVerificationLevel.SVL_1, userId, now,
            now.plusSeconds(300))));

        client.post().uri("/api/security/sessions/{sessionId}/step-up", sessionId)
            .header("X-Ikaros-Actor-Id", userId.toString()).exchange().expectStatus().isAccepted();
        client.post().uri("/api/security/sessions/{sessionId}/step-up/{challengeId}/verify", sessionId, challengeId)
            .header("X-Ikaros-Actor-Id", userId.toString()).bodyValue(Map.of("code", "123456"))
            .exchange().expectStatus().isOk();
        verify(stepUpService).issueEmailOtp(userId, sessionId);
    }
}
