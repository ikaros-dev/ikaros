package run.ikaros.security;

import java.util.List;
import java.util.UUID;

/** 已通过 JWT 签名校验的请求主体。 */
public record JwtPrincipal(UUID actorId, UUID sessionId, List<String> permissions) {
    public static final String EXCHANGE_ATTRIBUTE = JwtPrincipal.class.getName();
}
