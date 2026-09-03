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
        if (isAttachmentContentPath(path) && hasDeliveryGrant(exchange)) return chain.filter(exchange);
        if (!(path.startsWith("/api/resources") || path.startsWith("/api/resources")
            || path.startsWith("/api/storage/providers") || path.startsWith("/api/storage/providers")
            || path.startsWith("/api/admin/storage-providers")
            || path.startsWith("/api/admin/blobs")
            || path.startsWith("/api/admin/delivery-providers")
            || path.startsWith("/api/admin/restore-budget-policy")
            || path.startsWith("/api/storage/restore-budget")
            || path.startsWith("/api/storage/placements")
            || path.startsWith("/api/attachments/") && (path.contains("/restore-requests")
                || path.contains("/availability") || path.contains("/delivery-grants") || path.contains("/content"))
            || path.startsWith("/api/media/seasons/") && path.contains("/restore-requests")
            || path.startsWith("/api/restore-requests")
            || path.startsWith("/api/storage/restore-requests")
            || path.startsWith("/api/delivery-leases")
            || path.startsWith("/api/ingestion/sources") || path.startsWith("/api/ingestion/sources")
            || path.startsWith("/api/users") || path.startsWith("/api/admin/users")
            || path.startsWith("/api/roles") || path.startsWith("/api/admin/roles")
            || path.startsWith("/api/permissions") || path.startsWith("/api/admin/permissions"))) {
            return chain.filter(exchange);
        }
        PlatformPermission permission = permission(exchange.getRequest().getMethod().name(), path);
        return PrincipalContexts.current()
            .flatMap(context -> {
                if (context.sessionId() == null) {
                    return reject(exchange, HttpStatus.UNAUTHORIZED);
                }
                SecurityPolicy policy = policy(permission);
                return accessControl.require(context.actorId(), context.sessionId(), policy)
                    .then(chain.filter(exchange));
            })
            .switchIfEmpty(reject(exchange, HttpStatus.UNAUTHORIZED));
    }

    private PlatformPermission permission(String method, String path) {
        if (path.contains("/roles/") && path.contains("/permissions")) {
            return PlatformPermission.SYSTEM_ROLE_MANAGE;
        }
        if (path.contains("/users/") && path.contains("/roles/")) {
            return PlatformPermission.SYSTEM_ROLE_MANAGE;
        }
        if (path.contains("/permissions")) return PlatformPermission.SYSTEM_ROLE_READ;
        if (path.contains("/roles")) {
            return "GET".equals(method) ? PlatformPermission.SYSTEM_ROLE_READ
                : PlatformPermission.SYSTEM_ROLE_MANAGE;
        }
        if (path.contains("/sessions")) {
            return "GET".equals(method) ? PlatformPermission.SYSTEM_SESSION_READ
                : PlatformPermission.SYSTEM_SESSION_MANAGE;
        }
        if (path.contains("/users")) {
            return "GET".equals(method) ? PlatformPermission.SYSTEM_USER_READ
                : PlatformPermission.SYSTEM_USER_MANAGE;
        }
        if (path.contains("/admin/delivery-providers")) {
            return "GET".equals(method) ? PlatformPermission.STORAGE_DELIVERY_READ
                : PlatformPermission.STORAGE_DELIVERY_MANAGE;
        }
        if (path.contains("restore-budget")) return PlatformPermission.STORAGE_TIERING_MANAGE;
        if (path.contains("/storage/placements")) return PlatformPermission.STORAGE_TIERING_MANAGE;
        if (path.contains("/restore-requests")) {
            return "POST".equals(method) && (path.endsWith("/restore-requests")
                || path.endsWith("/restore-requests/attachments"))
                ? PlatformPermission.STORAGE_RESTORE_REQUEST : PlatformPermission.STORAGE_RESTORE_READ;
        }
        if (path.contains("/availability") || path.contains("/delivery-grants")
            || path.contains("/delivery-leases") || path.contains("/content")) {
            return PlatformPermission.RESOURCE_READ;
        }
        if (path.contains("/storage/providers") || path.contains("/admin/storage-providers")) {
            return "GET".equals(method) ? PlatformPermission.STORAGE_PROVIDER_READ
                : PlatformPermission.STORAGE_PROVIDER_MANAGE;
        }
        if (path.contains("/admin/blobs")) return PlatformPermission.STORAGE_PROVIDER_READ;
        if (path.contains("/ingestion/sources")) return PlatformPermission.INGESTION_SOURCE_MANAGE;
        if ("GET".equals(method)) return PlatformPermission.RESOURCE_READ;
        if ("DELETE".equals(method)) return PlatformPermission.RESOURCE_DELETE;
        return PlatformPermission.RESOURCE_WRITE;
    }

    private SecurityPolicy policy(PlatformPermission permission) {
        boolean highRisk = permission == PlatformPermission.SYSTEM_USER_MANAGE
            || permission == PlatformPermission.SYSTEM_ROLE_MANAGE
            || permission == PlatformPermission.SYSTEM_SESSION_MANAGE
            || permission == PlatformPermission.STORAGE_PROVIDER_MANAGE
            || permission == PlatformPermission.STORAGE_DELIVERY_MANAGE
            || permission == PlatformPermission.STORAGE_TIERING_MANAGE
            || permission == PlatformPermission.STORAGE_RESTORE_MANAGE
            || permission == PlatformPermission.INGESTION_SOURCE_MANAGE;
        return new SecurityPolicy("resource.http", permission,
            highRisk ? SecurityVerificationLevel.SVL_2 : SecurityVerificationLevel.SVL_0, highRisk);
    }

    private boolean hasDeliveryGrant(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("X-Ikaros-Delivery-Grant");
        String query = exchange.getRequest().getQueryParams().getFirst("delivery_grant");
        return (header != null && !header.isBlank()) || (query != null && !query.isBlank());
    }

    private boolean isAttachmentContentPath(String path) {
        return (path.startsWith("/api/attachments/") || path.startsWith("/api/attachments/"))
            && path.endsWith("/content");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
