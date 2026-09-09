package run.ikaros.authentication.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.authentication.PlatformUserEntity;
import run.ikaros.authentication.PlatformUserRepository;
import run.ikaros.authentication.UserStatus;

/** 验证真实 HTTP 邮件渠道的成功、失败与 Secret 传递边界。 */
class HttpEmailOtpDeliveryTest {
    @Test
    void sendsOtpThroughConfiguredGateway() {
        UUID userId = UUID.randomUUID();
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        Instant now = Instant.now();
        when(users.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice",
            "alice@example.com", UserStatus.ACTIVE, now, now, null, 0L)));
        ExchangeFunction exchange = request -> {
            assertThat(request.url().toString()).isEqualTo("https://mailer.example/send");
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer secret");
            return Mono.just(ClientResponse.create(HttpStatus.ACCEPTED).build());
        };
        HttpEmailOtpDelivery delivery = new HttpEmailOtpDelivery(users,
            WebClient.builder().exchangeFunction(exchange),
            new EmailOtpDeliveryProperties("HTTP", "https://mailer.example/send", "noreply@example.com", "secret"));

        StepVerifier.create(delivery.deliver(userId, "123456", VerificationPurpose.LOGIN_STEP_UP))
            .verifyComplete();
    }

    @Test
    void turnsGatewayFailureIntoNonSensitiveError() {
        UUID userId = UUID.randomUUID();
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        Instant now = Instant.now();
        when(users.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice",
            "alice@example.com", UserStatus.ACTIVE, now, now, null, 0L)));
        ExchangeFunction exchange = request -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY)
            .header(HttpHeaders.CONTENT_TYPE, "text/plain").body("provider secret 123456").build());
        HttpEmailOtpDelivery delivery = new HttpEmailOtpDelivery(users,
            WebClient.builder().exchangeFunction(exchange),
            new EmailOtpDeliveryProperties("HTTP", "https://mailer.example/send", "noreply@example.com", null));

        StepVerifier.create(delivery.deliver(userId, "123456", VerificationPurpose.LOGIN_STEP_UP))
            .expectErrorSatisfies(error -> {
                assertThat(error).hasMessage("验证码邮件投递失败");
                assertThat(error).hasMessageNotContaining("123456");
                assertThat(error).hasMessageNotContaining("provider secret");
            }).verify();
    }
}
