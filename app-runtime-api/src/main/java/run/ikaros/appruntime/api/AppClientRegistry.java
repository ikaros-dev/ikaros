package run.ikaros.appruntime.api;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** App Runtime 负责的 Client Registration 能力。 */
public interface AppClientRegistry {
    Mono<AppClientRegistrationView> register(UUID actorId, RegisterAppClientRequest request);

    Mono<AppClientRegistrationView> disable(UUID actorId, String clientId);

    Mono<AppClientRegistrationView> get(String clientId);

    Flux<AppClientRegistrationView> listByApp(String appId);
}
