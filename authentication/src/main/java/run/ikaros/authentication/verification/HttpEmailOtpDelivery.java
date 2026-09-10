package run.ikaros.authentication.verification;

import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import run.ikaros.authentication.PlatformUserRepository;

/** 通过部署方配置的 HTTP 邮件网关投递 OTP，不记录验证码或网关响应正文。 */
@Component
@ConditionalOnProperty(prefix = "ikaros.security.verification.email", name = "delivery", havingValue = "HTTP")
@EnableConfigurationProperties(EmailOtpDeliveryProperties.class)
public class HttpEmailOtpDelivery implements EmailOtpDelivery {
    private final PlatformUserRepository userRepository;
    private final WebClient client;
    private final EmailOtpDeliveryProperties properties;

    public HttpEmailOtpDelivery(PlatformUserRepository userRepository, WebClient.Builder clientBuilder,
                                EmailOtpDeliveryProperties properties) {
        this.userRepository = userRepository;
        this.client = clientBuilder.build();
        this.properties = properties;
    }

    @Override
    public Mono<Void> deliver(UUID userId, String code, VerificationPurpose purpose) {
        return userRepository.findById(userId)
            .map(user -> user.email())
            .filter(email -> email != null && !email.isBlank())
            .switchIfEmpty(Mono.error(new IllegalStateException("验证码投递目标不可用")))
            .flatMap(email -> client.post()
                .uri(properties.endpoint())
                .headers(headers -> {
                    if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
                        headers.setBearerAuth(properties.apiKey());
                    }
                })
                .bodyValue(Map.of("from", properties.from(), "to", email,
                    "subject", "Ikaros 安全验证码", "text", message(code, purpose)))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                    Mono.error(new IllegalStateException("验证码邮件投递失败")))
                .toBodilessEntity()
                .then());
    }

    private String message(String code, VerificationPurpose purpose) {
        return "Ikaros 操作验证码：" + code + "。用途：" + purpose
            + "。验证码 5 分钟内有效，请勿向他人透露。";
    }
}
