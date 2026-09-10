package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EbookImportRepository extends ReactiveCrudRepository<EbookImportEntity, UUID> {
    Flux<EbookImportEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Mono<EbookImportEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    Mono<EbookImportEntity> findByOwnerIdAndIdempotencyKey(UUID ownerId, String idempotencyKey);
}
