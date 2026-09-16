package run.ikaros.authentication.verification;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** 当前开发阶段的无投递实现；开启后在本地日志记录短信验证码。 */
@Component
@ConditionalOnProperty(prefix = "ikaros.security.verification.sms", name = "delivery",
    havingValue = "NOOP", matchIfMissing = true)
public class NoopSmsOtpDelivery implements SmsOtpDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoopSmsOtpDelivery.class);

    private final boolean logCode;

    public NoopSmsOtpDelivery(
        @Value("${ikaros.security.verification.sms.log-code:true}") boolean logCode) {
        this.logCode = logCode;
    }

    @Override
    public Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose) {
        if (logCode) {
            LOGGER.warn("NOOP SMS OTP generated for local development: userId={}, purpose={}, code={}",
                userId, purpose, code);
        }
        return Mono.empty();
    }
}
