package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MediaDeliveryGrantRepository extends ReactiveCrudRepository<MediaDeliveryGrantEntity, UUID> {
    Mono<MediaDeliveryGrantEntity> findByTokenHash(String tokenHash);
    Mono<MediaDeliveryGrantEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
