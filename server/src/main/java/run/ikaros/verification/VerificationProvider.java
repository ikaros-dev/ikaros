package run.ikaros.verification;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 所有安全认证 Provider 共享的挑战、验证与取消扩展点。
 */
public interface VerificationProvider {
    /**
     * 获取 Provider 实现的验证方式。
     *
     * @return 验证方式
     */
    VerificationMethod method();

    /**
     * 发起受用途绑定的验证挑战。
     *
     * @param userId 用户标识
     * @param request 挑战请求
     * @return 挑战摘要
     */
    Mono<VerificationChallengeView> issue(UUID userId, IssueVerificationRequest request);

    /**
     * 校验并一次性消费验证挑战。
     *
     * @param userId 用户标识
     * @param challengeId 挑战标识
     * @param request 验证码输入
     * @return 标准验证结果
     */
    Mono<VerificationResult> verify(UUID userId, UUID challengeId, VerifyOtpRequest request);

    /**
     * 取消尚未完成的验证挑战。
     *
     * @param userId 用户标识
     * @param challengeId 挑战标识
     * @return 完成信号
     */
    Mono<Void> cancel(UUID userId, UUID challengeId);
}
