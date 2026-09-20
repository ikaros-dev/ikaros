package run.ikaros.appruntime.api;

import java.time.Instant;

/** 当前 Instance 上一个 Server App 的安装与运行状态。 */
public record AppInstallationView(
    String appId,
    String packageVersion,
    AppLifecycleState lifecycleState,
    String platformApiMin,
    String platformApiMax,
    String configurationState,
    String failureCode,
    long version,
    Instant installedAt,
    Instant enabledAt,
    Instant disabledAt,
    Instant updatedAt
) { }
