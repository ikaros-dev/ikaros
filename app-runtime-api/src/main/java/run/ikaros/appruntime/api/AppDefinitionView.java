package run.ikaros.appruntime.api;

import java.util.List;

/** App Manifest 中由 Platform Registry 持有的稳定定义。 */
public record AppDefinitionView(
    String appId,
    String name,
    String publisher,
    String manifestVersion,
    List<Integer> supportedApiMajors,
    List<String> scopes
) {
    public AppDefinitionView {
        supportedApiMajors = supportedApiMajors == null ? List.of() : List.copyOf(supportedApiMajors);
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
}
