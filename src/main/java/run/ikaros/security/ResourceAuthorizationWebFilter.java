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
        if (!(path.startsWith("/api/v2/resources") || path.startsWith("/api/resources")
            || path.startsWith("/api/v2/storage/providers") || path.startsWith("/api/storage/providers")
            || path.startsWith("/api/v2/admin/storage-providers")
            || path.startsWith("/api/v2/admin/delivery-providers")
            || path.startsWith("/api/v2/admin/restore-budget-policy")
            || path.startsWith("/api/v2/storage/restore-budget")
            || path.startsWith("/api/v2/attachments/") && (path.contains("/restore-requests")
                || path.contains("/availability") || path.contains("/delivery-grants") || path.contains("/content"))
            || path.startsWith("/api/v2/media/seasons/") && path.contains("/restore-requests")
            || path.startsWith("/api/v2/restore-requests")
            || path.startsWith("/api/v2/storage/restore-requests")
            || path.startsWith("/api/v2/delivery-leases")
            || path.startsWith("/api/v2/ingestion/sources") || path.startsWith("/api/ingestion/sources"))) {
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
        if (path.contains("/admin/delivery-providers")) {
            return "GET".equals(method) ? PlatformPermission.STORAGE_DELIVERY_READ
                : PlatformPermission.STORAGE_DELIVERY_MANAGE;
        }
        if (path.contains("restore-budget")) return PlatformPermission.STORAGE_TIERING_MANAGE;
        if (path.contains("/restore-requests")) {
            return "POST".equals(method) && (path.endsWith("/restore-requests")
                || path.endsWith("/restore-requests/attachments"))
                ? PlatformPermission.STORAGE_RESTORE_REQUEST : PlatformPermission.STORAGE_RESTORE_READ;
        }
        if (path.contains("/availability") || path.contains("/delivery-grants")
            || path.contains("/delivery-leases") || path.contains("/content")) {
            return PlatformPermission.RESOURCE_READ;
        }
        if (path.contains("/storage/providers")) return PlatformPermission.STORAGE_PROVIDER_MANAGE;
        if (path.contains("/ingestion/sources")) return PlatformPermission.INGESTION_SOURCE_MANAGE;
        if ("GET".equals(method)) return PlatformPermission.RESOURCE_READ;
        if ("DELETE".equals(method)) return PlatformPermission.RESOURCE_DELETE;
        return PlatformPermission.RESOURCE_WRITE;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
