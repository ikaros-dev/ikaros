package run.ikaros.resource;

import java.util.UUID;
import java.util.Collection;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistence boundary for Resource membership roles. */
public interface UserResourceRepository extends ReactiveCrudRepository<UserResourceEntity, UUID> {
    Mono<UserResourceEntity> findByResourceIdAndUserId(UUID resourceId, UUID userId);

    Mono<UserResourceEntity> findByResourceIdAndUserIdAndRoleIn(UUID resourceId, UUID userId,
                                                                 Collection<String> roles);

    Mono<UserResourceEntity> findFirstByResourceIdAndRole(UUID resourceId, String role);

    Flux<UserResourceEntity> findAllByResourceId(UUID resourceId);
}
