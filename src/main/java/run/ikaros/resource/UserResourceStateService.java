package run.ikaros.resource;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface UserResourceStateService {
    Mono<UserResourceStateView> get(UUID userId, UUID resourceId);
    Mono<UserResourceStateView> set(UUID userId, UUID resourceId, UserResourceStateRequest request);
}
