package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ScanRunRepository extends ReactiveCrudRepository<ScanRunEntity, UUID> {
    Flux<ScanRunEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Mono<ScanRunEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
