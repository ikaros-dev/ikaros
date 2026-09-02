package run.ikaros.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.ConflictException;
import run.ikaros.identity.SecuritySessionEntity;
import run.ikaros.identity.SecuritySessionRepository;
import run.ikaros.identity.SecuritySessionService;
import run.ikaros.identity.SecurityVerificationLevel;

/** 验证 OTP 只能提升其绑定的活跃会话。 */
class DefaultStepUpVerificationServiceTest {
    private SecuritySessionRepository sessionRepository;
    private SecuritySessionService sessionService;
    private EmailOtpVerificationProvider otpProvider;
    private VerificationChallengeRepository challengeRepository;
    private DefaultStepUpVerificationService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SecuritySessionRepository.class);
        sessionService = mock(SecuritySessionService.class);
        otpProvider = mock(EmailOtpVerificationProvider.class);
        challengeRepository = mock(VerificationChallengeRepository.class);
        service = new DefaultStepUpVerificationService(sessionRepository, sessionService, otpProvider,
            challengeRepository);
    }

    @Test
    void issuesLoginStepUpChallengeBoundToActiveSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        when(sessionRepository.findById(sessionId)).thenReturn(Mono.just(session(sessionId, userId, now)));
        VerificationChallengeView view = new VerificationChallengeView(UUID.randomUUID(), VerificationMethod.EMAIL_OTP,
            VerificationPurpose.LOGIN_STEP_UP, now.plusSeconds(300), VerificationChallengeStatus.ISSUED);
        when(otpProvider.issue(eq(userId), any())).thenReturn(Mono.just(view));

        StepVerifier.create(service.issueEmailOtp(userId, sessionId)).expectNext(view).verifyComplete();
        verify(otpProvider).issue(userId, new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP,
            sessionId.toString()));
    }

    @Test
    void verifiesBoundChallengeThenRaisesTheSameSessionSvl() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, sessionId.toString(), "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        VerificationResult result = new VerificationResult(challengeId, VerificationMethod.EMAIL_OTP,
            SecurityVerificationLevel.SVL_1, userId, now, now.plusSeconds(300));
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));
        when(otpProvider.verify(userId, challengeId, new VerifyOtpRequest("123456"))).thenReturn(Mono.just(result));
        when(sessionService.stepUp(userId, sessionId, SecurityVerificationLevel.SVL_1, result.expiresAt()))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.verifyEmailOtp(userId, sessionId, challengeId, new VerifyOtpRequest("123456")))
            .expectNext(result)
            .verifyComplete();
        verify(sessionService).stepUp(userId, sessionId, SecurityVerificationLevel.SVL_1, result.expiresAt());
    }

    @Test
    void rejectsChallengeBoundToAnotherSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, UUID.randomUUID().toString(), "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));

        StepVerifier.create(service.verifyEmailOtp(userId, sessionId, challengeId, new VerifyOtpRequest("123456")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ConflictException.class);
                assertThat(error).hasMessage("验证码挑战未绑定到当前会话");
            })
            .verify();
    }

    private SecuritySessionEntity session(UUID sessionId, UUID userId, Instant now) {
        return new SecuritySessionEntity(sessionId, userId, "PASSWORD", 0, null, null, now.plusSeconds(3600), null,
            now, now, 0L);
    }
}
