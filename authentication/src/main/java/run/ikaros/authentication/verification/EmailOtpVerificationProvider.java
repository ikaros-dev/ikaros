package run.ikaros.authentication.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.AuditService;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.authentication.PlatformUserRepository;
import run.ikaros.authentication.UserStatus;
import run.ikaros.authentication.api.SecurityVerificationLevel;

/**
 * Email OTP Provider，负责短时挑战、一次性验证、失败锁定和发起频率限制。
 */
@Service
@EnableConfigurationProperties(OtpVerificationProperties.class)
public class EmailOtpVerificationProvider implements VerificationProvider {
    /** 未配置 `ikaros.security.verification.grant-ttl` 时，Step-up Grant 的内置有效期。 */
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(5);

    private final PlatformUserRepository userRepository;
    private final VerificationChallengeRepository challengeRepository;
    private final OtpCodeGenerator codeGenerator;
    private final OtpHasher otpHasher;
    private final EmailOtpDelivery delivery;
    private final AuditService auditService;
    private final Duration grantTtl;
    private final OtpVerificationProperties otp;

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
        this(userRepository, challengeRepository, codeGenerator, otpHasher, delivery, auditService,
            VERIFICATION_TTL, OtpVerificationProperties.defaults());
    }

    /**
     * 创建 Email OTP Provider，并允许通过配置覆盖 Step-up Grant 有效期。
     *
     * @param grantTtl Verification Grant 有效期；默认 PT5M
     * @param otp OTP 挑战策略（有效期、发起频率窗口与上限、最大验证次数）
     */
    @Autowired
    public EmailOtpVerificationProvider(PlatformUserRepository userRepository,
                                        VerificationChallengeRepository challengeRepository,
                                        OtpCodeGenerator codeGenerator, OtpHasher otpHasher,
                                        EmailOtpDelivery delivery, AuditService auditService,
                                        @Value("${ikaros.security.verification.grant-ttl:PT5M}") Duration grantTtl,
                                        OtpVerificationProperties otp) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.codeGenerator = codeGenerator;
        this.otpHasher = otpHasher;
        this.delivery = delivery;
        this.auditService = auditService;
        this.grantTtl = grantTtl == null ? VERIFICATION_TTL : grantTtl;
        this.otp = otp == null ? OtpVerificationProperties.defaults() : otp;
    }

    @Override
    public VerificationMethod method() {
        return VerificationMethod.EMAIL_OTP;
    }

    @Override
    public Mono<VerificationChallengeView> issue(UUID userId, IssueVerificationRequest request) {
        Instant now = Instant.now();
        return activeEmailUser(userId)
            .then(challengeRepository.countByUserIdAndIssuedAtAfter(userId, now.minus(otp.issueWindow())))
            .flatMap(count -> count >= otp.maxIssuesPerWindow()
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
                    .then(auditService.record(new AuditEventCommand(AuditActorType.USER, userId,
                        "security.verification.succeed", "VERIFICATION_CHALLENGE", challengeId,
                        AuditResult.SUCCESS, AuditRiskLevel.SENSITIVE, "{\"verification_level\":\"SVL_2\"}", 1,
                        null)))
                    .thenReturn(new VerificationResult(challengeId, method(), SecurityVerificationLevel.SVL_2, userId,
                        now, now.plus(grantTtl)));
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
            request.targetReference(), otpHasher.hash(code), now, now.plus(otp.ttl()), 0, otp.maxAttempts(), null,
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
            challenge.status(), null);
    }
}
