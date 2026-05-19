package run.ikaros.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GlobalExceptionHandlerConfigTest {

    @Mock
    private ServerWebExchange exchange;
    @Mock
    private WebFilterChain chain;

    private GlobalExceptionHandlerConfig filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new GlobalExceptionHandlerConfig();
    }

    @Test
    void constructor_noArg_createsInstance() {
        assertThat(filter).isNotNull();
        assertThat(filter).isInstanceOf(GlobalExceptionHandlerConfig.class);
    }

    @Test
    void filter_chainsToNextWhenNoError() {
        // Arrange: chain.filter returns empty Mono (no error)
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert: result should complete successfully without error
        StepVerifier.create(result)
            .verifyComplete();

        verify(chain).filter(exchange);
    }
}
