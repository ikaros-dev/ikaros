package run.ikaros.appruntime.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Server App 安装、启停和卸载的 Application Command 契约。 */
public interface AppLifecycleCommand {
    Mono<AppInstallationView> install(UUID actorId, InstallAppRequest request);

    Mono<AppInstallationView> enable(UUID actorId, String appId);

    Mono<AppInstallationView> disable(UUID actorId, String appId, String reasonCode);

    Mono<AppInstallationView> uninstall(UUID actorId, String appId, AppDataPolicy dataPolicy);
}
