package run.ikaros.authentication.verification;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 当前开发阶段的无投递实现；只有显式开启时才在本地日志记录验证码。
 */
@Component
@EnableConfigurationProperties(EmailOtpDeliveryProperties.class)
@ConditionalOnProperty(prefix = "ikaros.security.verification.email", name = "delivery",
    havingValue = "NOOP", matchIfMissing = true)
public class NoopEmailOtpDelivery implements EmailOtpDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoopEmailOtpDelivery.class);

    private final EmailOtpDeliveryProperties properties;

    public NoopEmailOtpDelivery(EmailOtpDeliveryProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose) {
        if (properties.logCode()) {
            LOGGER.warn("NOOP email OTP generated for local development: userId={}, purpose={}, code={}",
                userId, purpose, code);
        }
        return Mono.empty();
    }
}
