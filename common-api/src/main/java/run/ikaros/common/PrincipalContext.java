package run.ikaros.common;

import java.util.UUID;

/** 请求级身份上下文；业务代码不应把 HTTP Header 当作长期授权凭据。 */
public record PrincipalContext(UUID actorId, UUID tokenId, String requestId,
                               String correlationId, String causationId, boolean systemPrincipal) {
    public PrincipalContext(UUID actorId, UUID tokenId, String requestId,
                            String correlationId, boolean systemPrincipal) {
        this(actorId, tokenId, requestId, correlationId, null, systemPrincipal);
    }

    public static final String CONTEXT_KEY = PrincipalContext.class.getName();
}
