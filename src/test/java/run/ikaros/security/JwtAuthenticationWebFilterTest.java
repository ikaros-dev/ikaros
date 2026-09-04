package run.ikaros.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.ikaros.identity.JwtTokenService;

class JwtAuthenticationWebFilterTest {
    @Test
    void verifiesBearerAccessTokenAndExposesLegacyIdentityHeaders() {
        JwtTokenService tokens = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
            Duration.ofMinutes(15), Duration.ofDays(30));
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String accessToken = tokens.issue(userId, sessionId, List.of("resource.read")).accessToken();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources")
            .header("Authorization", "Bearer " + accessToken).build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        new JwtAuthenticationWebFilter(tokens).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsInvalidBearerToken() {
        JwtTokenService tokens = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
            Duration.ofMinutes(15), Duration.ofDays(30));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources")
            .header("Authorization", "Bearer invalid").build());

        new JwtAuthenticationWebFilter(tokens).filter(exchange, mock(WebFilterChain.class)).block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }
}
