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
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.identity.PlatformUserEntity;
import run.ikaros.identity.PlatformUserRepository;
import run.ikaros.identity.SecurityVerificationLevel;
import run.ikaros.identity.UserStatus;

/** 验证 Email OTP 的挑战、一次性消费、锁定和取消规则。 */
class EmailOtpVerificationProviderTest {
    private PlatformUserRepository userRepository;
    private VerificationChallengeRepository challengeRepository;
    private OtpCodeGenerator codeGenerator;
    private OtpHasher otpHasher;
    private EmailOtpDelivery delivery;
    private AuditService auditService;
    private EmailOtpVerificationProvider provider;

    @BeforeEach
    void setUp() {
        userRepository = mock(PlatformUserRepository.class);
        challengeRepository = mock(VerificationChallengeRepository.class);
        codeGenerator = mock(OtpCodeGenerator.class);
        otpHasher = mock(OtpHasher.class);
        delivery = mock(EmailOtpDelivery.class);
        auditService = mock(AuditService.class);
        provider = new EmailOtpVerificationProvider(userRepository, challengeRepository, codeGenerator, otpHasher,
            delivery, auditService);
    }

    @Test
    void identifiesItselfAsEmailOtpProvider() {
        assertThat(provider.method()).isEqualTo(VerificationMethod.EMAIL_OTP);
    }

    @Test
    void issuesBoundChallengeWithoutPersistingPlaintextOtp() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        PlatformUserEntity user = new PlatformUserEntity(userId, "alice", "Alice", "alice@example.com",
            UserStatus.ACTIVE, now, now, null, 0L);
        VerificationChallengeEntity saved = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, "session-1", "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(challengeRepository.countByUserIdAndIssuedAtAfter(eq(userId), any())).thenReturn(Mono.just(0L));
        when(codeGenerator.generate()).thenReturn("123456");
        when(otpHasher.hash("123456")).thenReturn("digest");
        when(challengeRepository.save(any())).thenReturn(Mono.just(saved));
        when(delivery.deliver(userId, "123456", VerificationPurpose.LOGIN_STEP_UP)).thenReturn(Mono.empty());
        when(auditService.record(eq(userId), eq("security.verification.issue"), eq("VERIFICATION_CHALLENGE"),
            eq(challengeId), eq("{}"))).thenReturn(Mono.empty());

        StepVerifier.create(provider.issue(userId, new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP,
                "session-1")))
            .assertNext(view -> assertThat(view.id()).isEqualTo(challengeId))
            .verifyComplete();
        verify(delivery).deliver(userId, "123456", VerificationPurpose.LOGIN_STEP_UP);
    }

    @Test
    void consumesMatchingOtpAndReturnsSvlOneResult() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.LOGIN_STEP_UP, null, "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));
        when(otpHasher.matches("123456", "digest")).thenReturn(true);
        when(challengeRepository.save(any())).thenReturn(Mono.just(challenge));
        when(auditService.record(eq(userId), eq("security.verification.succeed"), eq("VERIFICATION_CHALLENGE"),
            eq(challengeId), eq("{}"))).thenReturn(Mono.empty());

        StepVerifier.create(provider.verify(userId, challengeId, new VerifyOtpRequest("123456")))
            .assertNext(result -> assertThat(result.achievedSvl()).isEqualTo(SecurityVerificationLevel.SVL_1))
            .verifyComplete();
    }

    @Test
    void locksChallengeAtMaximumFailedAttempts() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.RESET_SECURE_KEY, null, "digest", now,
            now.plusSeconds(300), 4, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));
        when(otpHasher.matches("000000", "digest")).thenReturn(false);
        when(challengeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(auditService.record(eq(userId), eq("security.verification.failed"), eq("VERIFICATION_CHALLENGE"),
            eq(challengeId), eq("{}"))).thenReturn(Mono.empty());

        StepVerifier.create(provider.verify(userId, challengeId, new VerifyOtpRequest("000000")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ConflictException.class);
                assertThat(error).hasMessage("验证码错误次数过多，挑战已锁定");
            })
            .verify();
    }

    @Test
    void cancelsIssuedChallenge() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.EMAIL_OTP, VerificationPurpose.CHANGE_SECURITY_SETTING, null, "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));
        when(challengeRepository.save(any())).thenReturn(Mono.just(challenge));
        when(auditService.record(eq(userId), eq("security.verification.cancel"), eq("VERIFICATION_CHALLENGE"),
            eq(challengeId), eq("{}"))).thenReturn(Mono.empty());

        StepVerifier.create(provider.cancel(userId, challengeId)).verifyComplete();
        verify(challengeRepository).save(any(VerificationChallengeEntity.class));
    }
}
