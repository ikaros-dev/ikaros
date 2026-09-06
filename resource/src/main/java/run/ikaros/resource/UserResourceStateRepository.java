package run.ikaros.resource;

import run.ikaros.resource.api.*;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserResourceStateRepository extends ReactiveCrudRepository<UserResourceStateEntity, UUID> {
    Mono<UserResourceStateEntity> findByUserIdAndResourceId(UUID userId, UUID resourceId);
}
