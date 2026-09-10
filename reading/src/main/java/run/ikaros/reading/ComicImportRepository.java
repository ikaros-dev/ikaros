package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ComicImportRepository extends ReactiveCrudRepository<ComicImportEntity, UUID> {
    Flux<ComicImportEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Mono<ComicImportEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    Mono<ComicImportEntity> findByOwnerIdAndIdempotencyKey(UUID ownerId, String idempotencyKey);
}
