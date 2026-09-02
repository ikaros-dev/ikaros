package run.ikaros.security;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** 将认证层传入的主体信息收敛到 Reactor Context，并生成请求/关联 ID。 */
@Component
public class PrincipalContextWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String actor = exchange.getRequest().getHeaders().getFirst("X-Ikaros-Actor-Id");
        if (actor == null || actor.isBlank()) {
            return chain.filter(exchange);
        }
        try {
            UUID actorId = UUID.fromString(actor);
            String session = exchange.getRequest().getHeaders().getFirst("X-Ikaros-Session-Id");
            UUID sessionId = session == null || session.isBlank() ? null : UUID.fromString(session);
            String requestId = headerOrNew(exchange, "X-Request-Id");
            String correlationId = headerOrNew(exchange, "X-Correlation-Id");
            PrincipalContext context = new PrincipalContext(actorId, sessionId, requestId, correlationId, false);
            exchange.getResponse().getHeaders().set("X-Request-Id", requestId);
            return chain.filter(exchange).contextWrite(ctx -> ctx.put(PrincipalContext.CONTEXT_KEY, context));
        } catch (IllegalArgumentException invalidIdentity) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
    }

    private String headerOrNew(ServerWebExchange exchange, String name) {
        String value = exchange.getRequest().getHeaders().getFirst(name);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}
