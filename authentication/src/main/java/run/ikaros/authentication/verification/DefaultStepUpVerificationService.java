package run.ikaros.authentication.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.authentication.JwtTokenService;
import run.ikaros.authentication.PlatformUserEntity;
import run.ikaros.authentication.PlatformUserRepository;
import run.ikaros.authentication.UserStatus;
import run.ikaros.authentication.api.SecurityVerificationLevel;

/**
 * 默认 Step-up 协调服务，强制验证码用途与验证目标完全匹配。
 */
@Service
public class DefaultStepUpVerificationService implements StepUpVerificationService {
    private final PlatformUserRepository userRepository;
    private final EmailOtpVerificationProvider emailOtpProvider;
    private final VerificationChallengeRepository challengeRepository;
    private final JwtTokenService tokens;
    private final Duration reuseWindow;

    /**
     * 创建 Step-up 协调服务。
     *
     * @param userRepository 用户仓储
     * @param emailOtpProvider Email OTP Provider
     * @param challengeRepository 验证挑战仓储
     * @param tokens JWT 签发服务
     */
    public DefaultStepUpVerificationService(PlatformUserRepository userRepository,
                                            EmailOtpVerificationProvider emailOtpProvider,
                                            VerificationChallengeRepository challengeRepository,
                                            JwtTokenService tokens) {
        this(userRepository, emailOtpProvider, challengeRepository, tokens, Duration.ofHours(4));
    }

    @Autowired
    public DefaultStepUpVerificationService(PlatformUserRepository userRepository,
                                            EmailOtpVerificationProvider emailOtpProvider,
                                            VerificationChallengeRepository challengeRepository,
                                            JwtTokenService tokens,
                                            @Value("${ikaros.security.verification.email.reuse-window:PT4H}")
                                            Duration reuseWindow) {
        this.userRepository = userRepository;
        this.emailOtpProvider = emailOtpProvider;
        this.challengeRepository = challengeRepository;
        this.tokens = tokens;
        this.reuseWindow = reuseWindow;
    }

    @Override
    public Mono<VerificationChallengeView> issueEmailOtp(UUID userId) {
        Instant now = Instant.now();
        if (reuseWindow.isZero() || reuseWindow.isNegative()) {
            return emailOtpProvider.issue(userId, new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP, null));
        }
        return userRepository.findById(userId)
            .filter(user -> user.status() == UserStatus.ACTIVE)
            .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或已停用")))
            .flatMap(user -> challengeRepository
                .findFirstByUserIdAndPurposeAndStatusAndConsumedAtAfterOrderByConsumedAtDesc(
                    userId, VerificationPurpose.LOGIN_STEP_UP, VerificationChallengeStatus.VERIFIED,
                    now.minus(reuseWindow))
                .map(challenge -> new VerificationChallengeView(null, VerificationMethod.EMAIL_OTP,
                    VerificationPurpose.LOGIN_STEP_UP, challenge.consumedAt().plus(reuseWindow),
                    VerificationChallengeStatus.VERIFIED,
                    tokens.issueVerificationGrant(user.id(), user.securityVersion(), VerificationPurpose.LOGIN_STEP_UP,
                        null, SecurityVerificationLevel.SVL_1.value(), challenge.consumedAt(),
                        challenge.consumedAt().plus(reuseWindow))))
                .switchIfEmpty(Mono.defer(() -> emailOtpProvider.issue(userId,
                    new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP, null)))));
    }

    @Override
    public Mono<VerificationResult> verifyEmailOtp(UUID userId, UUID challengeId, VerifyOtpRequest request) {
        return boundStepUpChallenge(userId, challengeId)
            .then(Mono.defer(() -> userRepository.findById(userId))
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或已停用")))
                .flatMap(user -> emailOtpProvider.verify(userId, challengeId, request)
                    .map(result -> {
                        String grant = tokens.issueVerificationGrant(user.id(), user.securityVersion(),
                            VerificationPurpose.LOGIN_STEP_UP, null, result.achievedSvl().value(),
                            result.verifiedAt(), result.expiresAt());
                        return new VerificationResult(result.challengeId(), result.method(), result.achievedSvl(),
                            result.subjectId(), result.verifiedAt(), result.expiresAt(), grant);
                    }))); 
    }

    @Override
    public Mono<Void> cancelEmailOtp(UUID userId, UUID challengeId) {
        return boundStepUpChallenge(userId, challengeId)
            .then(emailOtpProvider.cancel(userId, challengeId));
    }

    private Mono<Void> boundStepUpChallenge(UUID userId, UUID challengeId) {
        return challengeRepository.findById(challengeId)
            .filter(challenge -> challenge.userId().equals(userId)
                && challenge.purpose() == VerificationPurpose.LOGIN_STEP_UP
                && (challenge.targetReference() == null || challenge.targetReference().isBlank()))
            .switchIfEmpty(Mono.error(new ConflictException("验证码挑战未绑定到当前用户的 Step-up 用途")))
            .then();
    }
}
