package run.ikaros.security;

import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import run.ikaros.identity.AccessControlService;
import run.ikaros.identity.PlatformPermission;
import run.ikaros.identity.SecurityPolicy;
import run.ikaros.identity.SecurityVerificationLevel;

/** Resource HTTP 入口的统一 RBAC + Session 校验。 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ResourceAuthorizationWebFilter implements WebFilter {
    private final AccessControlService accessControl;

    public ResourceAuthorizationWebFilter(AccessControlService accessControl) {
        this.accessControl = accessControl;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!(path.startsWith("/api/v2/resources") || path.startsWith("/api/resources"))) {
            return chain.filter(exchange);
        }
        PlatformPermission permission = permission(exchange.getRequest().getMethod().name(), path);
        return PrincipalContexts.current()
            .flatMap(context -> {
                if (context.sessionId() == null) {
                    return reject(exchange, HttpStatus.UNAUTHORIZED);
                }
                SecurityPolicy policy = new SecurityPolicy("resource.http", permission,
                    SecurityVerificationLevel.SVL_0, false);
                return accessControl.require(context.actorId(), context.sessionId(), policy)
                    .then(chain.filter(exchange));
            })
            .switchIfEmpty(reject(exchange, HttpStatus.UNAUTHORIZED));
    }

    private PlatformPermission permission(String method, String path) {
        if ("GET".equals(method)) return PlatformPermission.RESOURCE_READ;
        if ("DELETE".equals(method)) return PlatformPermission.RESOURCE_DELETE;
        return PlatformPermission.RESOURCE_WRITE;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
