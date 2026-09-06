package run.ikaros.authentication.verification;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 将用途绑定的验证结果转换为短期 Step-up Verification Grant 的业务边界。
 */
public interface StepUpVerificationService {
    /**
     * 为指定活跃用户发起 Email OTP Step-up 挑战。
     *
     * @param userId 当前用户标识
     * @return 挑战摘要
     */
    Mono<VerificationChallengeView> issueEmailOtp(UUID userId);

    /**
     * 验证挑战并为当前用户签发短期 Step-up Verification Grant。
     *
     * @param userId 当前用户标识
     * @param challengeId 验证挑战标识
     * @param request OTP 输入
     * @return 验证结果
     */
    Mono<VerificationResult> verifyEmailOtp(UUID userId, UUID challengeId, VerifyOtpRequest request);
}
