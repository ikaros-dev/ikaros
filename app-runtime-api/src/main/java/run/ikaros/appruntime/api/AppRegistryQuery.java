package run.ikaros.appruntime.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** App Runtime 对其他模块公开的只读 Registry 能力。 */
public interface AppRegistryQuery {
    Mono<AppDefinitionView> definition(String appId);

    Mono<AppInstallationView> installation(String appId);

    Flux<AppInstallationView> installations();

    Flux<String> scopes(String appId);
}
