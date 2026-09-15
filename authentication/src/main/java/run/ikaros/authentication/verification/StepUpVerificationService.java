package run.ikaros.authentication.verification;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 将用途绑定的验证结果转换为短期 Step-up Verification Grant 的业务边界。
 */
public interface StepUpVerificationService {
    /**
     * 为指定活跃用户发起 Email OTP Step-up 挑战；命中同账号最近成功验证的复用窗口时直接返回新的 Grant。
     *
     * @param userId 当前用户标识
     * @return 挑战摘要
     */
    Mono<VerificationChallengeView> issueEmailOtp(UUID userId);

    /** 为指定活跃用户发起 SMS OTP Step-up 挑战并达到 SVL-2。 */
    Mono<VerificationChallengeView> issueSmsOtp(UUID userId);

    /**
     * 验证挑战并为当前用户签发短期 Step-up Verification Grant。
     *
     * @param userId 当前用户标识
     * @param challengeId 验证挑战标识
     * @param request OTP 输入
     * @return 验证结果
     */
    Mono<VerificationResult> verifyEmailOtp(UUID userId, UUID challengeId, VerifyOtpRequest request);

    /** 验证 SMS OTP 并为当前用户签发 SVL-2 Step-up Grant。 */
    Mono<VerificationResult> verifySmsOtp(UUID userId, UUID challengeId, VerifyOtpRequest request);

    /**
     * 取消当前用户仍未使用的 Step-up 挑战。
     *
     * @param userId 当前用户标识
     * @param challengeId 验证挑战标识
     * @return 完成信号
     */
    Mono<Void> cancelEmailOtp(UUID userId, UUID challengeId);

    /** 取消当前用户仍未使用的 SMS OTP 挑战。 */
    Mono<Void> cancelSmsOtp(UUID userId, UUID challengeId);
}
