package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IngestionSourceRepository extends ReactiveCrudRepository<IngestionSourceEntity, UUID> {
    Flux<IngestionSourceEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
    Mono<IngestionSourceEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
