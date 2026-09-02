package run.ikaros.verification;

import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 当前开发阶段的无投递实现；它绝不记录或返回验证码明文。
 */
@Component
public class NoopEmailOtpDelivery implements EmailOtpDelivery {
    @Override
    public Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose) {
        return Mono.empty();
    }
}
