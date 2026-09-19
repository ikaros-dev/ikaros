package run.ikaros.appruntime.api;

import java.util.List;

/** 为某个 Server App 注册可识别 Client 的请求。 */
public record RegisterAppClientRequest(
    String clientId,
    String appId,
    String name,
    AppClientType clientType,
    String publisher,
    boolean official,
    List<String> redirectUris
) {
    public RegisterAppClientRequest {
        redirectUris = redirectUris == null ? List.of() : List.copyOf(redirectUris);
    }
}
