package run.ikaros.authentication.verification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email/SMS OTP 挑战策略；窗口、次数和有效期均可由部署配置覆盖，
 * 未配置或取非法值时回落到内置默认，避免出现无限制发送。
 */
@ConfigurationProperties(prefix = "ikaros.security.verification.otp")
public record OtpVerificationProperties(Duration ttl, Duration issueWindow, long maxIssuesPerWindow,
                                        int maxAttempts) {
    static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    static final Duration DEFAULT_ISSUE_WINDOW = Duration.ofMinutes(10);
    static final long DEFAULT_MAX_ISSUES_PER_WINDOW = 3;
    static final int DEFAULT_MAX_ATTEMPTS = 5;

    public OtpVerificationProperties {
        ttl = ttl == null || ttl.isZero() || ttl.isNegative() ? DEFAULT_TTL : ttl;
        issueWindow = issueWindow == null || issueWindow.isZero() || issueWindow.isNegative()
            ? DEFAULT_ISSUE_WINDOW : issueWindow;
        maxIssuesPerWindow = maxIssuesPerWindow <= 0 ? DEFAULT_MAX_ISSUES_PER_WINDOW : maxIssuesPerWindow;
        maxAttempts = maxAttempts <= 0 ? DEFAULT_MAX_ATTEMPTS : maxAttempts;
    }

    /** 内置默认策略，供未接入配置的场景（如单元测试）使用。 */
    static OtpVerificationProperties defaults() {
        return new OtpVerificationProperties(null, null, 0, 0);
    }
}
