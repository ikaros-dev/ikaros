package run.ikaros.authentication.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.ikaros.authentication.JwtTokenService;
import run.ikaros.authentication.PlatformUserEntity;
import run.ikaros.authentication.PlatformUserRepository;
import run.ikaros.authentication.UserStatus;

class JwtAuthenticationWebFilterTest {
    @Test
    void verifiesBearerAccessTokenAndExposesTokenIdentity() {
        JwtTokenService tokens = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
            Duration.ofMinutes(15), Duration.ofDays(30));
        UUID userId = UUID.randomUUID();
        String accessToken = tokens.issue(userId, 0L, List.of("resource.read")).accessToken();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources")
            .header("Authorization", "Bearer " + accessToken).build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        when(users.findById(any(UUID.class))).thenAnswer(invocation -> {
            UUID actualUserId = invocation.getArgument(0);
            return Mono.just(new PlatformUserEntity(actualUserId, "alice", "Alice", null,
                UserStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now(), null, 0L, 0L));
        });

        new JwtAuthenticationWebFilter(tokens, users).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsInvalidBearerToken() {
        JwtTokenService tokens = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
            Duration.ofMinutes(15), Duration.ofDays(30));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources")
            .header("Authorization", "Bearer invalid").build());

        new JwtAuthenticationWebFilter(tokens, mock(PlatformUserRepository.class)).filter(exchange,
            mock(WebFilterChain.class)).block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void rejectsWrongTypeExpiredDisabledAndStaleVersionTokens() {
        JwtTokenService tokens = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
            Duration.ofMinutes(15), Duration.ofDays(30));
        UUID userId = UUID.randomUUID();
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        PlatformUserRepository users = mock(PlatformUserRepository.class);
        when(users.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now(), null, 1L, 0L)));

        String refresh = tokens.issue(userId, 1L, List.of()).refreshToken();
        assertUnauthorized(tokens, users, chain, "Bearer " + refresh);

        String expired = new JwtTokenService("ikaros", "a-development-secret-with-at-least-32-characters",
            Duration.ofMinutes(-1), Duration.ofDays(30)).issue(userId, 1L, List.of()).accessToken();
        assertUnauthorized(tokens, users, chain, "Bearer " + expired);

        String disabled = tokens.issue(userId, 1L, List.of()).accessToken();
        when(users.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.DISABLED, java.time.Instant.now(), java.time.Instant.now(), null, 1L, 0L)));
        assertUnauthorized(tokens, users, chain, "Bearer " + disabled);

        String stale = tokens.issue(userId, 1L, List.of()).accessToken();
        when(users.findById(userId)).thenReturn(Mono.just(new PlatformUserEntity(userId, "alice", "Alice", null,
            UserStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now(), null, 2L, 0L)));
        assertUnauthorized(tokens, users, chain, "Bearer " + stale);
    }

    private void assertUnauthorized(JwtTokenService tokens, PlatformUserRepository users, WebFilterChain chain,
                                    String authorization) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/resources")
            .header("Authorization", authorization).build());
        new JwtAuthenticationWebFilter(tokens, users).filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }
}
