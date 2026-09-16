package run.ikaros.authentication.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.authentication.PlatformUserEntity;
import run.ikaros.authentication.PlatformUserRepository;
import run.ikaros.authentication.UserStatus;
import run.ikaros.authentication.api.SecurityVerificationLevel;
import run.ikaros.operations.api.AuditService;

/** 验证 SMS OTP 成功后达到 SVL-2。 */
class SmsOtpVerificationProviderTest {
    private PlatformUserRepository userRepository;
    private VerificationChallengeRepository challengeRepository;
    private OtpHasher otpHasher;
    private AuditService auditService;
    private SmsOtpVerificationProvider provider;

    @BeforeEach
    void setUp() {
        userRepository = mock(PlatformUserRepository.class);
        challengeRepository = mock(VerificationChallengeRepository.class);
        otpHasher = mock(OtpHasher.class);
        auditService = mock(AuditService.class);
        provider = new SmsOtpVerificationProvider(userRepository, challengeRepository, mock(OtpCodeGenerator.class),
            otpHasher, mock(SmsOtpDelivery.class), auditService);
    }

    @Test
    void identifiesItselfAsSmsOtp() {
        assertThat(provider.method()).isEqualTo(VerificationMethod.SMS_OTP);
    }

    @Test
    void consumesMatchingOtpAndReturnsSvlTwoResult() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Instant now = Instant.now();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(challengeId, userId,
            VerificationMethod.SMS_OTP, VerificationPurpose.LOGIN_STEP_UP, null, "digest", now,
            now.plusSeconds(300), 0, 5, null, VerificationChallengeStatus.ISSUED, 0L);
        when(challengeRepository.findById(challengeId)).thenReturn(Mono.just(challenge));
        when(otpHasher.matches("123456", "digest")).thenReturn(true);
        when(challengeRepository.save(any())).thenReturn(Mono.just(challenge));
        when(auditService.record(eq(userId), eq("security.verification.succeed"), eq("VERIFICATION_CHALLENGE"),
            eq(challengeId), eq("{}"))).thenReturn(Mono.empty());

        StepVerifier.create(provider.verify(userId, challengeId, new VerifyOtpRequest("123456")))
            .assertNext(result -> assertThat(result.achievedSvl()).isEqualTo(SecurityVerificationLevel.SVL_2))
            .verifyComplete();
    }

}
