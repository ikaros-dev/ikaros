package run.ikaros.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.identity.PlatformUserRepository;
import run.ikaros.identity.UserStatus;
import run.ikaros.identity.SecurityVerificationLevel;

/**
 * Email OTP Provider，负责短时挑战、一次性验证、失败锁定和发起频率限制。
 */
@Service
public class EmailOtpVerificationProvider implements VerificationProvider {
    /** OTP 有效期。 */
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    /** 单个用户的挑战频率窗口。 */
    private static final Duration ISSUE_WINDOW = Duration.ofMinutes(10);
    /** 单个用户在频率窗口内最大挑战数。 */
    private static final long MAX_ISSUES_PER_WINDOW = 3;
    /** 单个挑战允许的最大验证次数。 */
    private static final int MAX_ATTEMPTS = 5;
    /** 验证结果对 Step-up 的保证有效期。 */
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(5);

    private final PlatformUserRepository userRepository;
    private final VerificationChallengeRepository challengeRepository;
    private final OtpCodeGenerator codeGenerator;
    private final OtpHasher otpHasher;
    private final EmailOtpDelivery delivery;
    private final AuditService auditService;

    /**
     * 创建 Email OTP Provider。
     *
     * @param userRepository 用户仓储
     * @param challengeRepository 挑战仓储
     * @param codeGenerator OTP 生成器
     * @param otpHasher OTP 摘要器
     * @param delivery 专用邮件投递端口
     * @param auditService 审计服务
     */
    public EmailOtpVerificationProvider(PlatformUserRepository userRepository,
                                        VerificationChallengeRepository challengeRepository,
                                        OtpCodeGenerator codeGenerator, OtpHasher otpHasher,
                                        EmailOtpDelivery delivery, AuditService auditService) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.codeGenerator = codeGenerator;
        this.otpHasher = otpHasher;
        this.delivery = delivery;
        this.auditService = auditService;
    }

    @Override
    public VerificationMethod method() {
        return VerificationMethod.EMAIL_OTP;
    }

    @Override
    public Mono<VerificationChallengeView> issue(UUID userId, IssueVerificationRequest request) {
        Instant now = Instant.now();
        return activeEmailUser(userId)
            .then(challengeRepository.countByUserIdAndIssuedAtAfter(userId, now.minus(ISSUE_WINDOW)))
            .flatMap(count -> count >= MAX_ISSUES_PER_WINDOW
                ? Mono.error(new ConflictException("验证码发送过于频繁，请稍后重试"))
                : Mono.defer(() -> issueChallenge(userId, request, now)));
    }

    @Override
    public Mono<VerificationResult> verify(UUID userId, UUID challengeId, VerifyOtpRequest request) {
        Instant now = Instant.now();
        return ownedChallenge(userId, challengeId).flatMap(challenge -> {
            if (challenge.status() != VerificationChallengeStatus.ISSUED) {
                return Mono.error(new ConflictException("验证码挑战当前不可验证"));
            }
            if (!challenge.expiresAt().isAfter(now)) {
                return expire(challenge, userId);
            }
            if (otpHasher.matches(request.code(), challenge.otpDigest())) {
                VerificationChallengeEntity verified = new VerificationChallengeEntity(challenge.id(), challenge.userId(),
                    challenge.method(), challenge.purpose(), challenge.targetReference(), challenge.otpDigest(),
                    challenge.issuedAt(), challenge.expiresAt(), challenge.attemptCount(), challenge.maxAttempts(), now,
                    VerificationChallengeStatus.VERIFIED, challenge.version());
                return challengeRepository.save(verified)
                    .then(auditService.record(userId, "security.verification.succeed", "VERIFICATION_CHALLENGE",
                        challengeId, "{}"))
                    .thenReturn(new VerificationResult(challengeId, method(), SecurityVerificationLevel.SVL_1, userId,
                        now, now.plus(VERIFICATION_TTL)));
            }
            return failedAttempt(challenge, userId);
        });
    }

    @Override
    public Mono<Void> cancel(UUID userId, UUID challengeId) {
        return ownedChallenge(userId, challengeId).flatMap(challenge -> {
            if (challenge.status() != VerificationChallengeStatus.ISSUED) {
                return Mono.empty();
            }
            VerificationChallengeEntity cancelled = new VerificationChallengeEntity(challenge.id(), challenge.userId(),
                challenge.method(), challenge.purpose(), challenge.targetReference(), challenge.otpDigest(),
                challenge.issuedAt(), challenge.expiresAt(), challenge.attemptCount(), challenge.maxAttempts(), null,
                VerificationChallengeStatus.CANCELLED, challenge.version());
            return challengeRepository.save(cancelled).then();
        }).then(auditService.record(userId, "security.verification.cancel", "VERIFICATION_CHALLENGE", challengeId,
            "{}"));
    }

    private Mono<VerificationChallengeView> issueChallenge(UUID userId, IssueVerificationRequest request, Instant now) {
        String code = codeGenerator.generate();
        VerificationChallengeEntity challenge = new VerificationChallengeEntity(null, userId, method(), request.purpose(),
            request.targetReference(), otpHasher.hash(code), now, now.plus(OTP_TTL), 0, MAX_ATTEMPTS, null,
            VerificationChallengeStatus.ISSUED, null);
        return challengeRepository.save(challenge)
            .flatMap(saved -> delivery.deliver(userId, code, request.purpose())
                .then(auditService.record(userId, "security.verification.issue", "VERIFICATION_CHALLENGE", saved.id(),
                    "{}"))
                .thenReturn(toView(saved)));
    }

    private Mono<VerificationResult> expire(VerificationChallengeEntity challenge, UUID userId) {
        VerificationChallengeEntity expired = new VerificationChallengeEntity(challenge.id(), challenge.userId(),
            challenge.method(), challenge.purpose(), challenge.targetReference(), challenge.otpDigest(), challenge.issuedAt(),
            challenge.expiresAt(), challenge.attemptCount(), challenge.maxAttempts(), null,
            VerificationChallengeStatus.EXPIRED, challenge.version());
        return challengeRepository.save(expired)
            .then(Mono.error(new ConflictException("验证码已过期")));
    }

    private Mono<VerificationResult> failedAttempt(VerificationChallengeEntity challenge, UUID userId) {
        int attempts = challenge.attemptCount() + 1;
        VerificationChallengeStatus status = attempts >= challenge.maxAttempts()
            ? VerificationChallengeStatus.LOCKED : VerificationChallengeStatus.ISSUED;
        VerificationChallengeEntity updated = new VerificationChallengeEntity(challenge.id(), challenge.userId(),
            challenge.method(), challenge.purpose(), challenge.targetReference(), challenge.otpDigest(), challenge.issuedAt(),
            challenge.expiresAt(), attempts, challenge.maxAttempts(), null, status, challenge.version());
        return challengeRepository.save(updated)
            .then(auditService.record(userId, "security.verification.failed", "VERIFICATION_CHALLENGE", challenge.id(),
                "{}"))
            .then(Mono.error(new ConflictException(status == VerificationChallengeStatus.LOCKED
                ? "验证码错误次数过多，挑战已锁定" : "验证码错误")));
    }

    private Mono<Void> activeEmailUser(UUID userId) {
        return userRepository.findById(userId)
            .filter(user -> user.status() == UserStatus.ACTIVE && user.email() != null)
            .switchIfEmpty(Mono.error(new NotFoundException("用户不存在或未配置可验证邮箱")))
            .then();
    }

    private Mono<VerificationChallengeEntity> ownedChallenge(UUID userId, UUID challengeId) {
        return challengeRepository.findById(challengeId)
            .filter(challenge -> challenge.userId().equals(userId) && challenge.method() == method())
            .switchIfEmpty(Mono.error(new NotFoundException("验证挑战不存在")));
    }

    private VerificationChallengeView toView(VerificationChallengeEntity challenge) {
        return new VerificationChallengeView(challenge.id(), challenge.method(), challenge.purpose(), challenge.expiresAt(),
            challenge.status());
    }
}
