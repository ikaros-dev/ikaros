package run.ikaros.authentication.api;

import java.util.List;
import java.util.UUID;

/** 已完成认证校验的请求主体，不暴露 JWT 或 Web 框架实现细节。 */
public record AuthenticatedPrincipal(UUID actorId, UUID tokenId, long securityVersion,
                                    List<String> permissions) {
    public static final String EXCHANGE_ATTRIBUTE = AuthenticatedPrincipal.class.getName();

    public AuthenticatedPrincipal {
        permissions = List.copyOf(permissions);
    }
}
