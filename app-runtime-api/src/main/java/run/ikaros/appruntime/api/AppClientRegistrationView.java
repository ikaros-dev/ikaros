package run.ikaros.appruntime.api;

import java.time.Instant;
import java.util.List;

/** Client Registration 的公开只读表示。 */
public record AppClientRegistrationView(
    String clientId,
    String appId,
    String name,
    AppClientType clientType,
    String publisher,
    boolean official,
    String status,
    List<String> redirectUris,
    Instant createdAt,
    Instant updatedAt
) {
    public AppClientRegistrationView {
        redirectUris = redirectUris == null ? List.of() : List.copyOf(redirectUris);
    }
}
