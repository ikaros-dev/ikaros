package run.ikaros.authentication.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OtpVerificationPropertiesTest {
    @Test
    void fallsBackToBuiltInDefaultsWhenValuesAreMissingOrInvalid() {
        OtpVerificationProperties props = new OtpVerificationProperties(null, Duration.ZERO, -1, 0);
        assertThat(props.ttl()).isEqualTo(OtpVerificationProperties.DEFAULT_TTL);
        assertThat(props.issueWindow()).isEqualTo(OtpVerificationProperties.DEFAULT_ISSUE_WINDOW);
        assertThat(props.maxIssuesPerWindow()).isEqualTo(OtpVerificationProperties.DEFAULT_MAX_ISSUES_PER_WINDOW);
        assertThat(props.maxAttempts()).isEqualTo(OtpVerificationProperties.DEFAULT_MAX_ATTEMPTS);
    }

    @Test
    void keepsConfiguredValues() {
        OtpVerificationProperties props = new OtpVerificationProperties(Duration.ofMinutes(2),
            Duration.ofMinutes(30), 10, 9);
        assertThat(props.ttl()).isEqualTo(Duration.ofMinutes(2));
        assertThat(props.issueWindow()).isEqualTo(Duration.ofMinutes(30));
        assertThat(props.maxIssuesPerWindow()).isEqualTo(10);
        assertThat(props.maxAttempts()).isEqualTo(9);
    }
}
