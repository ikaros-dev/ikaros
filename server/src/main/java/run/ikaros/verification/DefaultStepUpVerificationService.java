package run.ikaros.verification;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.identity.JwtTokenService;
import run.ikaros.identity.PlatformUserEntity;
import run.ikaros.identity.PlatformUserRepository;
import run.ikaros.identity.UserStatus;

/**
 * 默认 Step-up 协调服务，强制验证码用途与 Session 目标完全匹配。
 */
@Service
public class DefaultStepUpVerificationService implements StepUpVerificationService {
    private final PlatformUserRepository userRepository;
    private final EmailOtpVerificationProvider emailOtpProvider;
    private final VerificationChallengeRepository challengeRepository;
    private final JwtTokenService tokens;

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
        this.userRepository = userRepository;
        this.emailOtpProvider = emailOtpProvider;
        this.challengeRepository = challengeRepository;
        this.tokens = tokens;
    }

    @Override
    public Mono<VerificationChallengeView> issueEmailOtp(UUID userId) {
        return emailOtpProvider.issue(userId, new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP, null));
    }

    @Override
    public Mono<VerificationResult> verifyEmailOtp(UUID userId, UUID challengeId, VerifyOtpRequest request) {
        return boundStepUpChallenge(userId, challengeId)
            .then(Mono.defer(() -> userRepository.findById(userId))
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或已停用"))))
            .zipWith(Mono.defer(() -> emailOtpProvider.verify(userId, challengeId, request)))
            .map(pair -> {
                PlatformUserEntity user = pair.getT1();
                VerificationResult result = pair.getT2();
                String grant = tokens.issueVerificationGrant(user.id(), user.securityVersion(),
                    VerificationPurpose.LOGIN_STEP_UP, null, result.achievedSvl().value(),
                    result.verifiedAt(), result.expiresAt());
                return new VerificationResult(result.challengeId(), result.method(), result.achievedSvl(),
                    result.subjectId(), result.verifiedAt(), result.expiresAt(), grant);
            });
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
