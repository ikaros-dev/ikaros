package run.ikaros.authentication.verification;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 当前开发阶段的无投递实现；它绝不记录或返回验证码明文。
 */
@Component
@ConditionalOnMissingBean(EmailOtpDelivery.class)
public class NoopEmailOtpDelivery implements EmailOtpDelivery {
    @Override
    public Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose) {
        return Mono.empty();
    }
}
