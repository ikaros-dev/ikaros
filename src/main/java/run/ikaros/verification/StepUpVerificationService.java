package run.ikaros.verification;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 将用途绑定的验证结果安全地应用到指定登录会话的业务边界。
 */
public interface StepUpVerificationService {
    /**
     * 为指定活跃会话发起 Email OTP Step-up 挑战。
     *
     * @param userId 当前用户标识
     * @param sessionId 需要提升的会话标识
     * @return 挑战摘要
     */
    Mono<VerificationChallengeView> issueEmailOtp(UUID userId, UUID sessionId);

    /**
     * 验证挑战并仅提升其绑定会话的安全等级。
     *
     * @param userId 当前用户标识
     * @param sessionId 需要提升的会话标识
     * @param challengeId 验证挑战标识
     * @param request OTP 输入
     * @return 验证结果
     */
    Mono<VerificationResult> verifyEmailOtp(UUID userId, UUID sessionId, UUID challengeId, VerifyOtpRequest request);
}
