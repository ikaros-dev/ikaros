package run.ikaros.authentication.verification;

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
import run.ikaros.authentication.JwtTokenService;
import run.ikaros.authentication.PlatformUserEntity;
import run.ikaros.authentication.PlatformUserRepository;
import run.ikaros.authentication.api.SecurityVerificationLevel;
import run.ikaros.authentication.UserStatus;

/** 验证 OTP 只能为当前用户生成短期 Step-up Grant。 */
class DefaultStepUpVerificationServiceTest {
    private PlatformUserRepository userRepository;
    private EmailOtpVerificationProvider otpProvider;
    private VerificationChallengeRepository challengeRepository;
    private DefaultStepUpVerificationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(PlatformUserRepository.class);
        otpProvider = mock(EmailOtpVerificationProvider.class);
        challengeRepository = mock(VerificationChallengeRepository.class);
        service = new DefaultStepUpVerificationService(userRepository, otpProvider, challengeRepository,
            new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
                java.time.Duration.ofMinutes(15), java.time.Duration.ofDays(30)));
    }

    @Test
    void issuesLoginStepUpChallengeBoundToCurrentUser() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeView view = new VerificationChallengeView(UUID.randomUUID(), VerificationMethod.EMAIL_OTP,
            VerificationPurpose.LOGIN_STEP_UP, now.plusSeconds(300), VerificationChallengeStatus.ISSUED);
        when(otpProvider.issue(eq(userId), any())).thenReturn(Mono.just(view));

        StepVerifier.create(service.issueEmailOtp(userId)).expectNext(view).verifyComplete();
        verify(otpProvider).issue(userId, new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP,
            null));
    }

    @Test
    void verifiesBoundChallengeThenReturnsVerificationGrant() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, null, "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        VerificationResult result = new VerificationResult(challengeId, VerificationMethod.EMAIL_OTP,
            SecurityVerificationLevel.SVL_1, userId, now, now.plusSeconds(300));
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));
        when(userRepository.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, now, now, null, 2L, 0L)));
        when(otpProvider.verify(userId, challengeId, new VerifyOtpRequest("123456"))).thenReturn(Mono.just(result));

        StepVerifier.create(service.verifyEmailOtp(userId, challengeId, new VerifyOtpRequest("123456")))
            .assertNext(actual -> {
                assertThat(actual.verificationGrant()).isNotBlank();
                assertThat(actual.verificationGrant()).isNotEqualTo(result.verificationGrant());
                JwtTokenService.VerificationGrantClaims claims = new JwtTokenService("ikaros",
                    "a-development-secret-with-at-least-32-characters", java.time.Duration.ofMinutes(15),
                    java.time.Duration.ofDays(30)).verifyVerificationGrant(actual.verificationGrant());
                assertThat(claims.userId()).isEqualTo(userId);
                assertThat(claims.purpose()).isEqualTo(VerificationPurpose.LOGIN_STEP_UP);
                assertThat(claims.achievedSvl()).isEqualTo(SecurityVerificationLevel.SVL_1.value());
                assertThat(claims.expiresAt()).isAfter(claims.verifiedAt());
            })
            .verifyComplete();
    }

    @Test
    void refusesLimitedOperationForInactiveUser() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, null, "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));
        when(userRepository.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.DISABLED, now, now, null, 2L, 0L)));

        StepVerifier.create(service.verifyEmailOtp(userId, challengeId, new VerifyOtpRequest("123456")))
            .expectError(run.ikaros.common.NotFoundException.class).verify();
        org.mockito.Mockito.verifyNoInteractions(otpProvider);
    }

    @Test
    void rejectsChallengeWithUnexpectedTargetReference() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, UUID.randomUUID().toString(), "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));

        StepVerifier.create(service.verifyEmailOtp(userId, challengeId, new VerifyOtpRequest("123456")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ConflictException.class);
                assertThat(error).hasMessage("验证码挑战未绑定到当前用户的 Step-up 用途");
            })
            .verify();
    }
}
