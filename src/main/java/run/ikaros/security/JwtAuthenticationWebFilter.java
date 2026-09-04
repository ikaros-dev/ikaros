package run.ikaros.security;

import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.ikaros.identity.JwtTokenService;

/** 校验 Bearer access token，并把 JWT 主体暴露给后续过滤器和旧业务控制器。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationWebFilter implements WebFilter {
    private final JwtTokenService tokens;

    public JwtAuthenticationWebFilter(JwtTokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) return chain.filter(exchange);
        if (isAttachmentContentPath(path) && hasDeliveryGrant(exchange)) return chain.filter(exchange);
        if (!path.startsWith("/api/") || isPublic(path)) return chain.filter(exchange);
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        try {
            JwtTokenService.Claims claims = tokens.verifyAccess(authorization.substring(7).trim());
            JwtPrincipal principal = new JwtPrincipal(claims.userId(), claims.sessionId(), claims.permissions());
            ServerWebExchange enriched = exchange.mutate().request(exchange.getRequest().mutate()
                .header("X-Ikaros-Actor-Id", claims.userId().toString())
                .header("X-Ikaros-Session-Id", claims.sessionId().toString()).build()).build();
            enriched.getAttributes().put(JwtPrincipal.EXCHANGE_ATTRIBUTE, principal);
            return chain.filter(enriched);
        } catch (RuntimeException invalidToken) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublic(String path) {
        return path.equals("/api/health/live") || path.equals("/api/health/ready")
            || path.equals("/api/auth/register") || path.equals("/api/auth/login")
            || path.equals("/api/auth/refresh-token") || path.equals("/api/refresh-token");
    }

    private boolean hasDeliveryGrant(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("X-Ikaros-Delivery-Grant");
        String query = exchange.getRequest().getQueryParams().getFirst("delivery_grant");
        return (header != null && !header.isBlank()) || (query != null && !query.isBlank());
    }

    private boolean isAttachmentContentPath(String path) {
        return path.startsWith("/api/attachments/") && path.endsWith("/content");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
