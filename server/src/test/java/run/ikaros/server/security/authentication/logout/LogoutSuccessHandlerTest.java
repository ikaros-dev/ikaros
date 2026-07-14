package run.ikaros.server.security.authentication.logout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LogoutSuccessHandlerTest {

    @Test
    void onLogoutSuccess() {
        LogoutSuccessHandler handler = new LogoutSuccessHandler();
        ServerHttpResponse response = new MockServerHttpResponse(new DefaultDataBufferFactory());
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getResponse()).thenReturn(response);
        WebFilterChain chain = mock(WebFilterChain.class);
        WebFilterExchange filterExchange = new WebFilterExchange(exchange, chain);
        Authentication authentication = mock(Authentication.class);

        StepVerifier.create(handler.onLogoutSuccess(filterExchange, authentication))
            .verifyComplete();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
            .isEqualTo(MediaType.APPLICATION_JSON);
    }
}
