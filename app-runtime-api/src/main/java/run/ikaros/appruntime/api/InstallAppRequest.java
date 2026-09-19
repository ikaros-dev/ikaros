package run.ikaros.appruntime.api;

import java.util.List;

/** 安装 Server App 时写入 App Registry 的 Manifest 子集。 */
public record InstallAppRequest(
    String appId,
    String name,
    String publisher,
    String manifestVersion,
    String packageVersion,
    List<Integer> supportedApiMajors,
    String platformApiMin,
    String platformApiMax,
    List<String> scopes
) {
    public InstallAppRequest {
        supportedApiMajors = supportedApiMajors == null ? List.of() : List.copyOf(supportedApiMajors);
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
}
