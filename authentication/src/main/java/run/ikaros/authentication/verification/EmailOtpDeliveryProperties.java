package run.ikaros.authentication.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** HTTP 邮件网关配置；apiKey 只允许来自部署环境的 Secret 配置。 */
@ConfigurationProperties(prefix = "ikaros.security.verification.email")
public record EmailOtpDeliveryProperties(String delivery, String endpoint, String from, String apiKey) {
    public boolean httpEnabled() {
        return "HTTP".equalsIgnoreCase(delivery) && endpoint != null && !endpoint.isBlank()
            && from != null && !from.isBlank();
    }
}
