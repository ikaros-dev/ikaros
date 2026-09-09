package run.ikaros.authentication.verification;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 当前开发阶段的无投递实现；它绝不记录或返回验证码明文。
 */
@Component
@ConditionalOnProperty(prefix = "ikaros.security.verification.email", name = "delivery",
    havingValue = "NOOP", matchIfMissing = true)
public class NoopEmailOtpDelivery implements EmailOtpDelivery {
    @Override
    public Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose) {
        return Mono.empty();
    }
}
