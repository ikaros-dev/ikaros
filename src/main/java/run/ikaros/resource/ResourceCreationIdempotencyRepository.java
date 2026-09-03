package run.ikaros.resource;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ResourceCreationIdempotencyRepository
    extends ReactiveCrudRepository<ResourceCreationIdempotencyEntity, UUID> {
    Mono<ResourceCreationIdempotencyEntity> findByOwnerIdAndIdempotencyKey(UUID ownerId, String idempotencyKey);
}
