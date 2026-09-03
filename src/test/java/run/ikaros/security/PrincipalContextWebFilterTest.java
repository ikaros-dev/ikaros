package run.ikaros.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class PrincipalContextWebFilterTest {
    @Test
    void propagatesPrincipalToReactorContext() {
        UUID actor = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
            .header("X-Ikaros-Actor-Id", actor.toString())
            .header("X-Ikaros-Session-Id", session.toString()).build());
        WebFilterChain chain = current -> PrincipalContexts.current().map(context -> {
            assertEquals(actor, context.actorId());
            assertEquals(session, context.sessionId());
            return context;
        }).then();
        new PrincipalContextWebFilter().filter(exchange, chain).block();
    }
}
