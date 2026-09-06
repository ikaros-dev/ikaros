package run.ikaros.resource.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface UserResourceStateService {
    Mono<UserResourceStateView> get(UUID userId, UUID resourceId);
    Mono<UserResourceStateView> set(UUID userId, UUID resourceId, UserResourceStateRequest request);
    Mono<UserResourceStateView> set(UUID userId, UUID resourceId, UserResourceStateRequest request,
                                    long expectedVersion);
}
