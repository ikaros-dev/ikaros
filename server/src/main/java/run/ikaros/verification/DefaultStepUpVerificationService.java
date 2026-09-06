package run.ikaros.verification;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.identity.SecuritySessionRepository;
import run.ikaros.identity.SecuritySessionService;

/**
 * 默认 Step-up 协调服务，强制验证码用途与 Session 目标完全匹配。
 */
@Service
public class DefaultStepUpVerificationService implements StepUpVerificationService {
    private final SecuritySessionRepository sessionRepository;
    private final SecuritySessionService sessionService;
    private final EmailOtpVerificationProvider emailOtpProvider;
    private final VerificationChallengeRepository challengeRepository;

    /**
     * 创建 Step-up 协调服务。
     *
     * @param sessionRepository 会话仓储
     * @param sessionService 会话服务
     * @param emailOtpProvider Email OTP Provider
     * @param challengeRepository 验证挑战仓储
     */
    public DefaultStepUpVerificationService(SecuritySessionRepository sessionRepository,
                                            SecuritySessionService sessionService,
                                            EmailOtpVerificationProvider emailOtpProvider,
                                            VerificationChallengeRepository challengeRepository) {
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
        this.emailOtpProvider = emailOtpProvider;
        this.challengeRepository = challengeRepository;
    }

    @Override
    public Mono<VerificationChallengeView> issueEmailOtp(UUID userId, UUID sessionId) {
        return activeOwnedSession(userId, sessionId)
            .then(emailOtpProvider.issue(userId, new IssueVerificationRequest(VerificationPurpose.LOGIN_STEP_UP,
                sessionId.toString())));
    }

    @Override
    public Mono<VerificationResult> verifyEmailOtp(UUID userId, UUID sessionId, UUID challengeId,
                                                    VerifyOtpRequest request) {
        return boundStepUpChallenge(userId, sessionId, challengeId)
            .then(Mono.defer(() -> emailOtpProvider.verify(userId, challengeId, request)))
            .flatMap(result -> sessionService.stepUp(userId, sessionId, result.achievedSvl(), result.expiresAt())
                .thenReturn(result));
    }

    private Mono<Void> activeOwnedSession(UUID userId, UUID sessionId) {
        Instant now = Instant.now();
        return sessionRepository.findById(sessionId)
            .filter(session -> session.userId().equals(userId) && session.revokedAt() == null
                && session.expiresAt().isAfter(now))
            .switchIfEmpty(Mono.error(new NotFoundException("活跃会话不存在")))
            .then();
    }

    private Mono<Void> boundStepUpChallenge(UUID userId, UUID sessionId, UUID challengeId) {
        return challengeRepository.findById(challengeId)
            .filter(challenge -> challenge.userId().equals(userId)
                && challenge.purpose() == VerificationPurpose.LOGIN_STEP_UP
                && sessionId.toString().equals(challenge.targetReference()))
            .switchIfEmpty(Mono.error(new ConflictException("验证码挑战未绑定到当前会话")))
            .then();
    }
}
