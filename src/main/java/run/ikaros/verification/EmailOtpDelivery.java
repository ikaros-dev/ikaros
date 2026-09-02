package run.ikaros.verification;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 将 OTP 直接投递给认证邮箱的端口，不得接入普通通知历史。
 */
public interface EmailOtpDelivery {
    /**
     * 投递一次性验证码。
     *
     * @param userId 接收用户标识
     * @param code 仅在当前调用链存在的验证码明文
     * @param purpose 验证用途
     * @return 投递完成信号
     */
    Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose);
}
