package run.ikaros.authentication.verification;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.authentication.JwtTokenService;
import run.ikaros.authentication.PlatformUserRepository;
import run.ikaros.authentication.UserStatus;

/**
 * 默认 Step-up 协调服务，强制验证码用途与验证目标完全匹配。
 */
@Service
public class DefaultStepUpVerificationService implements StepUpVerificationService {
    private final PlatformUserRepository userRepository;
    private final EmailOtpVerificationProvider emailOtpProvider;
    private final SmsOtpVerificationProvider smsOtpProvider;
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
        this(userRepository, emailOtpProvider, null, challengeRepository, tokens);
    }

    @Autowired
    public DefaultStepUpVerificationService(PlatformUserRepository userRepository,
                                            EmailOtpVerificationProvider emailOtpProvider,
                                            SmsOtpVerificationProvider smsOtpProvider,
                                            VerificationChallengeRepository challengeRepository,
                                            JwtTokenService tokens) {
        this.userRepository = userRepository;
        this.emailOtpProvider = emailOtpProvider;
        this.smsOtpProvider = smsOtpProvider;
        this.challengeRepository = challengeRepository;
        this.tokens = tokens;
    }

    @Override
    public Mono<VerificationChallengeView> issueEmailOtp(UUID userId) {
        return issueOtp(userId, emailOtpProvider);
    }

    @Override
    public Mono<VerificationChallengeView> issueSmsOtp(UUID userId) {
        return issueOtp(userId, smsOtpProvider);
    }

    private Mono<VerificationChallengeView> issueOtp(UUID userId, VerificationProvider provider) {
        if (provider == null) return Mono.error(new IllegalStateException("短信验证码服务不可用"));
        return userRepository.findById(userId)
            .filter(user -> user.status() == UserStatus.ACTIVE)
            .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或已停用")))
            .flatMap(user -> provider.issue(userId,
                new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP, null)));
    }

    @Override
    public Mono<VerificationResult> verifyEmailOtp(UUID userId, UUID challengeId, VerifyOtpRequest request) {
        return verifyOtp(userId, challengeId, request, emailOtpProvider);
    }

    @Override
    public Mono<VerificationResult> verifySmsOtp(UUID userId, UUID challengeId, VerifyOtpRequest request) {
        return verifyOtp(userId, challengeId, request, smsOtpProvider);
    }

    private Mono<VerificationResult> verifyOtp(UUID userId, UUID challengeId, VerifyOtpRequest request,
                                               VerificationProvider provider) {
        if (provider == null) return Mono.error(new IllegalStateException("短信验证码服务不可用"));
        return boundStepUpChallenge(userId, challengeId)
            .then(Mono.defer(() -> userRepository.findById(userId))
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或已停用")))
                .flatMap(user -> provider.verify(userId, challengeId, request)
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

    @Override
    public Mono<Void> cancelSmsOtp(UUID userId, UUID challengeId) {
        if (smsOtpProvider == null) return Mono.error(new IllegalStateException("短信验证码服务不可用"));
        return boundStepUpChallenge(userId, challengeId)
            .then(smsOtpProvider.cancel(userId, challengeId));
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
