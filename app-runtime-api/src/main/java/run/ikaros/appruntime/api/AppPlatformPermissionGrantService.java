package run.ikaros.appruntime.api;

import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Server App -> Platform Permission Grant 的公开能力。 */
public interface AppPlatformPermissionGrantService {
    Mono<Void> replace(UUID actorId, String appId, Set<String> permissionKeys);

    Flux<String> grantedPermissions(String appId);
}
